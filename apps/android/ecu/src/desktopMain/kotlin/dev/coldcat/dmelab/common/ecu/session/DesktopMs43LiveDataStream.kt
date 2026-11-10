package dev.coldcat.dmelab.common.ecu.session

import dev.coldcat.dmelab.common.ecu.model.LiveDataSelection
import dev.coldcat.dmelab.common.ecu.model.SelectedLiveData
import dev.coldcat.dmelab.ffi.LiveDataStreamControl
import dev.coldcat.dmelab.ffi.LiveDataStreamFailure
import dev.coldcat.dmelab.ffi.SelectedLiveDataListener
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/** One native stream; stop remains callable while the client's mutex is busy. */
@Single
internal class DesktopMs43LiveDataStream(
    private val access: DesktopMs43Access,
    @Named("ecu") private val ecu: CoroutineDispatcher,
) : Ms43LiveDataStream {
    // Admission, publication, stop and disposal run on the service's main dispatcher.
    private var control: LiveDataStreamControl? = null

    override fun stop() { nativeCall { control?.stop() } }

    override suspend fun run(
        adapterId: String,
        timeoutMs: UInt,
        selection: LiveDataSelection,
        onSample: suspend (SelectedLiveData) -> Unit,
        onRecovering: (ULong) -> Unit,
    ): Unit = nativeCall {
        coroutineScope {
            check(control == null)
            LiveDataStreamControl().use { signal ->
                control = signal
                val cancellation = launch(start = CoroutineStart.UNDISPATCHED) {
                    try { awaitCancellation() }
                    finally { signal.stop() }
                }
                try {
                    withContext(context = ecu) {
                        val callbackJob = currentCoroutineContext().job
                        var callbackFailure: Exception? = null
                        fun deliver(action: suspend () -> Unit) {
                            if (callbackFailure != null) return
                            try {
                                // Synchronous backpressure: persist each sample on IO before
                                // requesting another. Never dispatch this block to the busy ECU thread.
                                runBlocking(context = callbackJob) {
                                    try { action() }
                                    catch (exception: Exception) {
                                        // Catch inside the child coroutine so a file failure
                                        // cannot cancel its parent before it is rethrown below.
                                        callbackFailure = exception
                                        signal.stop()
                                    }
                                }
                            } catch (exception: Exception) {
                                // Preserve file/cancellation errors instead of UniFFI wrapping
                                // a thrown listener exception as DmeException.Transport.
                                if (callbackFailure == null) callbackFailure = exception
                                signal.stop()
                            }
                        }
                        try {
                            access.useNative(adapterId = adapterId) { client ->
                                client.native.streamLiveDataSelected(selection = selection.toNative(), timeoutMs = timeoutMs, control = signal,
                                    listener = object : SelectedLiveDataListener {
                                        override fun onSample(data: dev.coldcat.dmelab.ffi.SelectedLiveData) = deliver { onSample(data.toModel()) }
                                        override fun onRecovering(failure: LiveDataStreamFailure) = deliver { onRecovering(failure.consecutiveFailures) }
                                    })
                            }
                        } catch (exception: Exception) {
                            throw callbackFailure ?: exception
                        }
                        callbackFailure?.let { throw it }
                    }
                } finally {
                    // The native call and port cleanup have returned before control is disposed.
                    withContext(context = NonCancellable) { cancellation.cancelAndJoin() }
                    control = null
                }
            }
        }
    }
}
