package dev.coldcat.dmelab.features.firmware.read.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.coldcat.dmelab.common.ecu.model.FirmwareMode
import dev.coldcat.dmelab.common.session.domain.EcuConnection
import dev.coldcat.dmelab.common.session.domain.EcuConnectionState
import dev.coldcat.dmelab.common.session.domain.EcuOperations
import dev.coldcat.dmelab.common.session.domain.OperationFailure
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
internal class ReadFlashViewModel(
    private val connection: EcuConnection,
    private val operations: EcuOperations,
    mapper: ReadFlashScreenMapper,
) : ViewModel() {
    private val internalState = MutableStateFlow(ReadFlashInternalState(connection = connection.state.value, operation = operations.state.value))
    private var pendingDestination: Pair<FirmwareMode, Long>? = null
    private val commandChannel = Channel<ReadFlashScreenCommand>(capacity = Channel.BUFFERED)
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
                    )
                }
            }
        }
        viewModelScope.launch {
            operations.state.collect { current -> internalState.update { it.copy(operation = current) } }
        }
    }

    fun onEvent(event: ReadFlashScreenEvent) {
        when (event) {
            ReadFlashScreenEvent.OnBack -> commandChannel.trySend(ReadFlashScreenCommand.Back)
            is ReadFlashScreenEvent.OnMode -> if (operations.state.value.active == null) {
                internalState.update { it.copy(mode = event.mode, localFailure = null) }
            }
            ReadFlashScreenEvent.OnChooseDestination -> if (canStart()) {
                pendingDestination = internalState.value.mode to connection.state.value.sessionId
                val name = when (internalState.value.mode) {
                    FirmwareMode.Full -> "ms43-full.bin"
                    FirmwareMode.Calibration -> "ms43-calibration.bin"
                }
                commandChannel.trySend(ReadFlashScreenCommand.ChooseDestination(name))
            }
            is ReadFlashScreenEvent.OnDestination -> {
                internalState.update { it.copy(localFailure = null) }
                val pending = pendingDestination
                pendingDestination = null
                if (pending == null || pending.second != connection.state.value.sessionId) {
                    internalState.update { it.copy(localFailure = OperationFailure.Disconnected) }
                } else if (!canStart() || !operations.readFlash(mode = pending.first, destination = event.location)) {
                    internalState.update { it.copy(localFailure = OperationFailure.Busy) }
                }
            }
            ReadFlashScreenEvent.OnFileAccessFailure -> internalState.update { it.copy(localFailure = OperationFailure.FileAccess) }
        }
    }

    private fun canStart() = connection.state.value.operation is EcuConnectionState.Operation.Identified &&
        operations.state.value.active == null
}
