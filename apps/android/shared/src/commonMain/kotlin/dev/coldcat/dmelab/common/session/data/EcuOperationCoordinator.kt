package dev.coldcat.dmelab.common.session.data

import dev.coldcat.dmelab.common.platform.monotonicTimeNanos
import dev.coldcat.dmelab.common.platform.isPermissionFailure
import kotlin.time.Clock
import kotlinx.io.IOException
import dev.coldcat.dmelab.common.ecu.model.FirmwareMode
import dev.coldcat.dmelab.common.ecu.model.LiveDataSelection
import dev.coldcat.dmelab.common.ecu.model.Progress
import dev.coldcat.dmelab.common.ecu.model.SelectedLiveData
import dev.coldcat.dmelab.common.ecu.session.Ms43Access
import dev.coldcat.dmelab.common.ecu.session.Ms43LiveDataStream
import dev.coldcat.dmelab.common.ecu.session.ProgressListener
import dev.coldcat.dmelab.common.ecu.session.FirmwareValidator
import dev.coldcat.dmelab.common.session.domain.ActiveOperation
import dev.coldcat.dmelab.common.session.domain.ConnectionProblem
import dev.coldcat.dmelab.common.session.domain.DiagnosticResult
import dev.coldcat.dmelab.common.session.domain.DtcRecord
import dev.coldcat.dmelab.common.session.domain.EcuConnection
import dev.coldcat.dmelab.common.session.domain.EcuConnectionState
import dev.coldcat.dmelab.common.session.domain.EcuOperations
import dev.coldcat.dmelab.common.session.domain.EcuOperationState
import dev.coldcat.dmelab.common.session.domain.EcuSessionLease
import dev.coldcat.dmelab.common.session.domain.FirmwareFileException
import dev.coldcat.dmelab.common.session.domain.FirmwareFiles
import dev.coldcat.dmelab.common.session.domain.FlashResult
import dev.coldcat.dmelab.common.session.domain.LiveDataLogs
import dev.coldcat.dmelab.common.session.domain.LiveDataTiming
import dev.coldcat.dmelab.common.session.domain.LiveDataWakeLock
import dev.coldcat.dmelab.common.session.domain.OperationFailure
import dev.coldcat.dmelab.common.session.domain.OperationHost
import dev.coldcat.dmelab.common.session.domain.OperationJournal
import dev.coldcat.dmelab.common.session.domain.OperationKind
import dev.coldcat.dmelab.common.session.domain.OperationProgress
import dev.coldcat.dmelab.common.session.domain.OperationRuntime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/** Main-thread admission, one reserved USB session, and service-owned execution. No command queue. */
@Single
internal class EcuOperationCoordinator(
    private val validator: FirmwareValidator,
    private val host: OperationHost,
    private val journal: OperationJournal,
    private val connection: EcuConnection,
    private val access: Ms43Access,
    private val files: FirmwareFiles,
    private val liveMapper: LiveDataValues,
    private val liveLogs: LiveDataLogs,
    private val liveStream: Ms43LiveDataStream,
    private val liveWakeLock: LiveDataWakeLock,
    @Named("ecu") private val ecu: CoroutineDispatcher,
    @Named("io") private val io: CoroutineDispatcher,
) : EcuOperations, OperationRuntime {
    private val mutableState = MutableStateFlow(
        EcuOperationState(failure = if (journal.wasRunning) OperationFailure.Interrupted else null),
    )
    override val state = mutableState.asStateFlow()
    private var pending: (suspend () -> Unit)? = null
    private var lease: EcuSessionLease? = null
    private var resultSessionId: Long? = null
    private var startedAtElapsedMillis = 0L

    override fun readDtcs(): Boolean = submit(kind = OperationKind.ReadDtc) { session ->
        val records = withContext(context = ecu) {
            access.use(adapterId = session.adapterId) { client ->
                client.readDtcs(timeoutMs = TIMEOUT_MS).map { record ->
                    DtcRecord(code = record.code.toInt(), frequency = record.frequency.toInt(), data = hex(record.data), raw = hex(record.raw))
                }
            }
        }
        ensureCurrent(session = session)
        mutableState.update { it.copy(dtcs = DiagnosticResult(value = records, updatedAt = Clock.System.now().toEpochMilliseconds())) }
    }

    override fun clearDtcs(expectedSessionId: Long): Boolean = submit(kind = OperationKind.ClearDtc, expectedSessionId = expectedSessionId) { session ->
        withContext(context = ecu) { access.use(adapterId = session.adapterId) { it.clearDtcs(timeoutMs = TIMEOUT_MS) } }
        ensureCurrent(session = session)
        mutableState.update { it.copy(dtcClear = DiagnosticResult(value = Unit, updatedAt = Clock.System.now().toEpochMilliseconds())) }
    }

    override fun readLiveData(selection: LiveDataSelection): Boolean = live(poll = false, selection = selection)
    override fun startLiveData(record: Boolean, selection: LiveDataSelection): Boolean = live(poll = true, record = record, selection = selection)
    override fun stopLiveData() {
        if (mutableState.value.active?.kind != OperationKind.LiveData) return
        liveStream.stop()
        mutableState.update { it.copy(active = it.active?.copy(stopping = true)) }
    }

    private fun live(poll: Boolean, record: Boolean = false, selection: LiveDataSelection): Boolean {
        if (!selection.isNotEmpty) return false
        val accepted = submit(kind = OperationKind.LiveData) { session ->
            if (mutableState.value.active?.stopping == true) return@submit
            var completed = false
            var failure: Throwable? = null
            try {
                if (record) logFile { liveLogs.start(selection = selection) }
                var firstCompletedNanos = 0L
                var previousCompletedNanos: Long? = null
                var sampleCount = 0L
                suspend fun publish(data: SelectedLiveData, startedNanos: Long?) {
                    val completedNanos = monotonicTimeNanos()
                    currentCoroutineContext().ensureActive()
                    ensureCurrent(session = session)
                    sampleCount++
                    if (sampleCount == 1L) firstCompletedNanos = completedNanos
                    val spanNanos = completedNanos - firstCompletedNanos
                    val frequency = if (poll && sampleCount > 1L && spanNanos > 0L) {
                        (sampleCount - 1L).toDouble() * NANOS_PER_SECOND / spanNanos
                    } else null
                    val timestamp = Clock.System.now().toEpochMilliseconds()
                    if (record) logFile {
                        liveLogs.append(data = data, timestampMillis = timestamp,
                            completedAtMillis = completedNanos / NANOS_PER_MILLISECOND)
                    }
                    val values = liveMapper.map(sample = data)
                    mutableState.update {
                        it.copy(
                            liveData = DiagnosticResult(value = values, updatedAt = timestamp),
                            liveDataTiming = LiveDataTiming(
                                readDurationMillis = startedNanos?.let { (completedNanos - it) / NANOS_PER_MILLISECOND },
                                sampleIntervalMillis = previousCompletedNanos?.let { (completedNanos - it) / NANOS_PER_MILLISECOND },
                                updateFrequencyHz = frequency,
                            ),
                            liveDataRecoveryCount = null,
                        )
                    }
                    previousCompletedNanos = completedNanos
                }
                ensureCurrent(session = session)
                if (mutableState.value.active?.stopping != true) {
                    if (poll) {
                        liveStream.run(adapterId = session.adapterId, timeoutMs = TIMEOUT_MS, selection = selection,
                            onSample = { publish(data = it, startedNanos = null) },
                            onRecovering = { failure ->
                                ensureCurrent(session = session)
                                mutableState.update { it.copy(liveDataRecoveryCount = failure) }
                            })
                    } else withContext(context = ecu) {
                        access.use(adapterId = session.adapterId) { client ->
                            val startedNanos = monotonicTimeNanos()
                            publish(data = client.readLiveDataSelected(selection = selection, timeoutMs = TIMEOUT_MS), startedNanos = startedNanos)
                        }
                    }
                }
                ensureCurrent(session = session)
                completed = true
            } catch (error: Throwable) {
                failure = error
                throw error
            } finally {
                if (record) withContext(context = kotlinx.coroutines.NonCancellable) {
                    try { logFile { liveLogs.finish(partial = !completed) } }
                    catch (exception: FirmwareFileException) {
                        val original = failure
                        if (original == null) throw exception
                        original.addSuppressed(exception)
                    }
                }
            }
        }
        if (accepted) {
            mutableState.update { it.copy(active = it.active?.copy(polling = poll, liveDataSelection = selection)) }
        }
        return accepted
    }

    private suspend fun logFile(action: suspend () -> Unit) {
        try { action() }
        catch (_: IOException) { throw FirmwareFileException(problem = OperationFailure.FileAccess) }
        catch (exception: Exception) {
            if (isPermissionFailure(exception)) throw FirmwareFileException(problem = OperationFailure.FileAccess)
            throw exception
        }
    }

    override fun readFlash(mode: FirmwareMode, destination: String): Boolean = submit(kind = OperationKind.ReadFlash) { session ->
        var byteCount = 0
        files.save(destination = destination) {
            ensureCurrent(session = session)
            val bytes = withContext(context = ecu) {
                access.use(adapterId = session.adapterId) { client ->
                    client.readFlash(mode = mode, timeoutMs = TIMEOUT_MS, listener = progressListener())
                }
            }
            ensureCurrent(session = session)
            byteCount = bytes.size
            mutableState.update { it.copy(active = it.active?.copy(progress = OperationProgress(stage = "saving", completed = 0, total = bytes.size.toLong()))) }
            bytes
        }
        mutableState.update { it.copy(flash = FlashResult(kind = OperationKind.ReadFlash, mode = mode, location = destination, byteCount = byteCount, completedAt = Clock.System.now().toEpochMilliseconds(), elapsedMillis = elapsedMillis())) }
    }

    override fun writeFlash(mode: FirmwareMode, source: String, expectedSessionId: Long, expectedFingerprint: String): Boolean =
        submit(kind = OperationKind.WriteFlash, expectedSessionId = expectedSessionId) { session ->
            val bytes = files.read(source = source)
            if (files.fingerprint(bytes = bytes) != expectedFingerprint) throw FirmwareFileException(problem = OperationFailure.InvalidImage)
            ensureCurrent(session = session)
            withContext(context = ecu) {
                validator.validateImage(image = bytes, mode = mode)
                access.use(adapterId = session.adapterId) { client ->
                    client.writeFlash(mode = mode, image = bytes, timeoutMs = TIMEOUT_MS, listener = progressListener())
                }
            }
            ensureCurrent(session = session)
            mutableState.update { it.copy(flash = FlashResult(kind = OperationKind.WriteFlash, mode = mode, location = source, byteCount = bytes.size, completedAt = Clock.System.now().toEpochMilliseconds(), elapsedMillis = elapsedMillis())) }
        }

    private fun submit(kind: OperationKind, expectedSessionId: Long? = null, work: suspend (EcuSessionLease) -> Unit): Boolean {
        if (mutableState.value.active != null) return false
        val session = connection.tryReserve()
        if (session == null || (expectedSessionId != null && expectedSessionId != session.sessionId)) {
            session?.let(connection::release)
            mutableState.update { it.copy(failure = OperationFailure.Disconnected, failureKind = kind) }
            return false
        }
        if (resultSessionId != session.sessionId) mutableState.value = EcuOperationState()
        resultSessionId = session.sessionId
        lease = session
        startedAtElapsedMillis = (monotonicTimeNanos() / NANOS_PER_MILLISECOND)
        mutableState.update {
            it.copy(active = ActiveOperation(kind = kind), resultSessionId = session.sessionId, failure = null, failureKind = null,
                dtcs = it.dtcs.copy(failure = null), dtcClear = it.dtcClear.copy(failure = null), liveData = it.liveData.copy(failure = null),
                liveDataTiming = if (kind == OperationKind.LiveData) null else it.liveDataTiming,
                liveDataRecoveryCount = null)
        }
        pending = {
            ensureCurrent(session = session)
            work(session)
        }
        try {
            host.start()
        } catch (exception: RuntimeException) {
            pending = null
            lease = null
            connection.release(lease = session)
            fail(kind = kind, problem = OperationFailure.Unexpected)
            mutableState.update { it.copy(active = null) }
            return false
        }
        return true
    }

    override fun rejectPending(problem: OperationFailure) {
        if (pending == null) return
        val current = lease ?: return
        val kind = mutableState.value.active?.kind ?: return
        pending = null
        lease = null
        connection.release(lease = current)
        fail(kind = kind, problem = problem)
        mutableState.update { it.copy(active = null) }
    }

    /** Called by the foreground service; blocking exchange finishes before the lease is released. */
    override fun runPending(scope: CoroutineScope): Job? {
        val work = pending ?: return null
        val session = lease ?: return null
        val kind = mutableState.value.active?.kind ?: return null
        val keepCpuAwake = kind == OperationKind.LiveData && mutableState.value.active?.polling == true
        pending = null
        return scope.launch {
            // The clock keeps advancing during blocking ECU calls and while no screen observes state.
            val timer = if (kind == OperationKind.ReadFlash || kind == OperationKind.WriteFlash) launch {
                while (isActive) {
                    mutableState.update { it.copy(active = it.active?.copy(elapsedMillis = elapsedMillis())) }
                    delay(timeMillis = TIMER_INTERVAL_MS)
                }
            } else null
            try {
                val journaled = journal.markRunning(running = true)
                if (!journaled) throw FirmwareFileException(problem = OperationFailure.FileAccess)
                if (keepCpuAwake) liveWakeLock.hold(action = work) else work()
            } catch (exception: CancellationException) {
                fail(kind = kind, problem = OperationFailure.Interrupted)
                throw exception
            } catch (exception: Exception) {
                val failure = OperationErrors.from(error = exception)
                fail(kind = kind, problem = failure)
                if (failure == OperationFailure.NeedsReconnect) connection.invalidate(problem = ConnectionProblem.NeedsReconnect)
                else if (failure == OperationFailure.Exchange) connection.invalidate(problem = ConnectionProblem.Exchange)
            } finally {
                timer?.cancel()
                try {
                    withContext(context = kotlinx.coroutines.NonCancellable + io) { journal.markRunning(running = false) }
                } finally {
                    connection.release(lease = session)
                    lease = null
                    mutableState.update { it.copy(active = null, liveDataRecoveryCount = null) }
                }
            }
        }
    }

    private fun fail(kind: OperationKind, problem: OperationFailure) {
        mutableState.update {
            when (kind) {
                OperationKind.ReadDtc -> it.copy(failure = problem, failureKind = kind, dtcs = it.dtcs.copy(failure = problem))
                OperationKind.ClearDtc -> it.copy(failure = problem, failureKind = kind, dtcClear = it.dtcClear.copy(failure = problem))
                OperationKind.LiveData -> it.copy(failure = problem, failureKind = kind, liveData = it.liveData.copy(failure = problem))
                OperationKind.ReadFlash, OperationKind.WriteFlash -> it.copy(failure = problem, failureKind = kind)
            }
        }
    }

    private fun ensureCurrent(session: EcuSessionLease) {
        if (connection.state.value.sessionId != session.sessionId || connection.state.value.operation !is EcuConnectionState.Operation.Identified) {
            throw FirmwareFileException(problem = OperationFailure.Disconnected)
        }
    }

    private fun progressListener(): ProgressListener = object : ProgressListener {
        override fun onProgress(progress: Progress) {
            mutableState.update {
                it.copy(active = it.active?.copy(progress = OperationProgress(stage = progress.stage, completed = progress.completed.toLong(), total = progress.total.toLong())))
            }
        }
    }

    private fun elapsedMillis(): Long = ((monotonicTimeNanos() / NANOS_PER_MILLISECOND) - startedAtElapsedMillis).coerceAtLeast(minimumValue = 0L)

    private fun hex(bytes: ByteArray): String = bytes.joinToString(separator = " ") { (it.toInt() and 0xff).toString(radix = 16).uppercase().padStart(length = 2, padChar = '0') }
    private companion object {
        const val TIMEOUT_MS = 2000u
        const val TIMER_INTERVAL_MS = 1000L
        const val NANOS_PER_MILLISECOND = 1_000_000L
        const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
