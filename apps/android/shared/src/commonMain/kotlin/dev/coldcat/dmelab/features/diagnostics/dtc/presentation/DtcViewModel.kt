package dev.coldcat.dmelab.features.diagnostics.dtc.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.coldcat.dmelab.common.ecu.model.UsbInventory
import dev.coldcat.dmelab.common.session.domain.EcuConnection
import dev.coldcat.dmelab.common.session.domain.EcuConnectionState.Operation
import dev.coldcat.dmelab.common.session.domain.EcuOperations
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class DtcViewModel(
    private val connection: EcuConnection,
    private val operations: EcuOperations,
    mapper: DtcScreenMapper,
) : ViewModel() {
    private val internalState = MutableStateFlow(DtcInternalState(
        connection = connection.state.value,
        operations = operations.state.value,
    ))
    private val commandChannel = Channel<DtcScreenCommand>(capacity = Channel.BUFFERED)
    val commands = commandChannel.receiveAsFlow()
    val state = internalState.map(mapper::getScreenState).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = mapper.getScreenState(internal = internalState.value),
    )

    init {
        viewModelScope.launch {
            combine(connection.state, operations.state) { session, operation -> session to operation }
                .collect { (session, operation) ->
                    internalState.update {
                        it.copy(
                            connection = session,
                            operations = operation,
                            confirmingSessionId = it.confirmingSessionId.takeIf { id -> id == session.sessionId },
                        )
                    }
                }
        }
    }

    fun onEvent(event: DtcScreenEvent) {
        when (event) {
            DtcScreenEvent.OnBack -> commandChannel.trySend(DtcScreenCommand.Back)
            DtcScreenEvent.OnRead -> operations.readDtcs()
            DtcScreenEvent.OnClear -> if (ready()) {
                internalState.update { it.copy(confirmingSessionId = connection.state.value.sessionId) }
            }
            DtcScreenEvent.OnDismissClear -> internalState.update { it.copy(confirmingSessionId = null) }
            DtcScreenEvent.OnConfirmClear -> {
                val expected = internalState.value.confirmingSessionId
                internalState.update { it.copy(confirmingSessionId = null) }
                if (expected != null && expected == connection.state.value.sessionId && ready()) {
                    operations.clearDtcs(expectedSessionId = expected)
                }
            }
        }
    }

    private fun ready(): Boolean = connection.state.value.operation is Operation.Identified &&
        operations.state.value.active == null &&
        (connection.state.value.inventory as? UsbInventory.Available)?.adapters?.any {
            it.id == connection.state.value.selectedId && it.hasPermission
        } == true
}
