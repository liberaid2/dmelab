package dev.coldcat.dmelab.features.firmware.write.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.coldcat.dmelab.common.ecu.model.FirmwareMode
import dev.coldcat.dmelab.common.session.domain.EcuConnection
import dev.coldcat.dmelab.common.session.domain.EcuConnectionState
import dev.coldcat.dmelab.common.session.domain.EcuOperations
import dev.coldcat.dmelab.common.session.domain.FirmwareFileException
import dev.coldcat.dmelab.common.session.domain.FirmwareFiles
import dev.coldcat.dmelab.common.session.domain.OperationFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class WriteFlashViewModel(
    private val connection: EcuConnection,
    private val operations: EcuOperations,
    private val files: FirmwareFiles,
    mapper: WriteFlashScreenMapper,
) : ViewModel() {
    private val internalState = MutableStateFlow(WriteFlashInternalState(connection = connection.state.value, operation = operations.state.value))
    private var pendingFile: Pair<FirmwareMode, Long>? = null
    private var externalFile: String? = null
    private val commandChannel = Channel<WriteFlashScreenCommand>(capacity = Channel.BUFFERED)
    val commands = commandChannel.receiveAsFlow()
    val state = internalState.map(mapper::getScreenState).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = mapper.getScreenState(internal = internalState.value),
    )

    init {
        viewModelScope.launch {
            connection.state.collect { current ->
                internalState.update {
                    it.copy(
                        connection = current,
                        localFailure = it.localFailure.takeIf { _ -> current.sessionId == it.connection.sessionId },
                        confirmation = it.confirmation && current.sessionId == it.reviewedSessionId,
                        image = it.image.takeIf { _ -> current.sessionId == it.connection.sessionId },
                    )
                }
            }
        }
        viewModelScope.launch {
            operations.state.collect { current ->
                internalState.update { it.copy(operation = current, confirmation = it.confirmation && current.active == null) }
            }
        }
    }

    fun onEvent(event: WriteFlashScreenEvent) {
        when (event) {
            WriteFlashScreenEvent.OnBack -> commandChannel.trySend(WriteFlashScreenCommand.Back)
            is WriteFlashScreenEvent.OnMode -> if (canStart()) {
                val source = internalState.value.image?.location
                internalState.update { it.copy(mode = event.mode, image = null, confirmation = false, localFailure = null) }
                if (source != null) inspect(location = source, mode = event.mode, sessionId = connection.state.value.sessionId)
            }
            is WriteFlashScreenEvent.OnExternalFile -> if (externalFile != event.location) {
                externalFile = event.location
                if (canStart()) inspect(location = event.location, mode = event.mode, sessionId = connection.state.value.sessionId)
                else internalState.update { it.copy(localFailure = OperationFailure.Busy) }
            }
            WriteFlashScreenEvent.OnChooseFile -> if (canStart()) {
                pendingFile = internalState.value.mode to connection.state.value.sessionId
                commandChannel.trySend(WriteFlashScreenCommand.ChooseFile)
            }
            is WriteFlashScreenEvent.OnFile -> {
                val pending = pendingFile
                pendingFile = null
                if (pending == null || pending.second != connection.state.value.sessionId) {
                    internalState.update { it.copy(localFailure = OperationFailure.Disconnected) }
                } else {
                    inspect(location = event.location, mode = pending.first, sessionId = pending.second)
                }
            }
            WriteFlashScreenEvent.OnRequestConfirmation -> if (canStart() && internalState.value.image != null) {
                internalState.update { it.copy(confirmation = true, reviewedSessionId = connection.state.value.sessionId) }
            }
            WriteFlashScreenEvent.OnDismissConfirmation -> internalState.update { it.copy(confirmation = false) }
            WriteFlashScreenEvent.OnConfirm -> confirm()
            WriteFlashScreenEvent.OnFileAccessFailure -> internalState.update { it.copy(localFailure = OperationFailure.FileAccess) }
        }
    }

    private fun inspect(location: String, mode: FirmwareMode, sessionId: Long) {
        if (!canStart()) return
        internalState.update { it.copy(mode = mode, inspecting = true, image = null, confirmation = false, localFailure = null) }
        viewModelScope.launch {
            try {
                val image = files.inspect(source = location, mode = mode)
                if (sessionId == connection.state.value.sessionId) internalState.update { it.copy(image = image) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: FirmwareFileException) {
                if (sessionId == connection.state.value.sessionId) {
                    internalState.update { it.copy(localFailure = failure.problem) }
                }
            } catch (_: Exception) {
                if (sessionId == connection.state.value.sessionId) {
                    internalState.update { it.copy(localFailure = OperationFailure.Unexpected) }
                }
            } finally {
                internalState.update { it.copy(inspecting = false) }
            }
        }
    }

    private fun confirm() {
        val snapshot = internalState.value
        val image = snapshot.image ?: return
        if (!snapshot.confirmation || !canStart() || snapshot.reviewedSessionId != connection.state.value.sessionId) return
        internalState.update { it.copy(confirmation = false, localFailure = null) }
        if (!operations.writeFlash(
                mode = image.mode,
                source = image.location,
                expectedSessionId = connection.state.value.sessionId,
                expectedFingerprint = image.fingerprint,
            )) internalState.update { it.copy(localFailure = OperationFailure.Busy) }
    }

    private fun canStart() = !internalState.value.inspecting &&
        connection.state.value.operation is EcuConnectionState.Operation.Identified && operations.state.value.active == null
}
