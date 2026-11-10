package dev.coldcat.dmelab.features.connection.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.coldcat.dmelab.common.ecu.model.UsbInventory
import dev.coldcat.dmelab.common.session.domain.EcuConnection
import dev.coldcat.dmelab.common.session.domain.EcuConnectionState.Operation
import dev.coldcat.dmelab.features.connection.presentation.ConnectionScreenState.Step
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/** Two-step onboarding; ECU work is owned by the shared connection, not this screen. */
@KoinViewModel
internal class ConnectionViewModel(
    private val connection: EcuConnection,
    mapper: ConnectionScreenMapper,
) : ViewModel() {
    private val commandChannel = Channel<ConnectionScreenCommand>(capacity = Channel.BUFFERED)
    val commands = commandChannel.receiveAsFlow()
    private val internalState = MutableStateFlow(
        ConnectionInternalState(connection = connection.state.value, step = Step.Cable),
    )
    val state: StateFlow<ConnectionScreenState> = internalState
        .map(mapper::getScreenState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), mapper.getScreenState(internal = internalState.value))

    init {
        viewModelScope.launch {
            connection.state.collect { current ->
                val selected = (current.inventory as? UsbInventory.Available)?.adapters
                    ?.firstOrNull { it.id == current.selectedId && it.hasPermission }
                internalState.update {
                    it.copy(
                        connection = current,
                        step = if (selected == null && current.operation !is Operation.Identifying) Step.Cable else it.step,
                    )
                }
            }
        }
    }

    fun onEvent(event: ConnectionScreenEvent) {
        when (event) {
            ConnectionScreenEvent.OnLogsClicked -> commandChannel.trySend(ConnectionScreenCommand.OpenLogs)
            ConnectionScreenEvent.OnContinueClicked -> {
                val current = internalState.value.connection
                val selected = (current.inventory as? UsbInventory.Available)?.adapters
                    ?.any { it.id == current.selectedId && it.hasPermission } == true
                if (selected || current.operation is Operation.Identifying) internalState.update { it.copy(step = Step.Ecu) }
            }
            ConnectionScreenEvent.OnIdentifyClicked -> connection.identify()
            ConnectionScreenEvent.OnPermissionClicked -> connection.requestPermission()
            ConnectionScreenEvent.OnRefreshClicked, ConnectionScreenEvent.OnResumed -> connection.refresh()
            ConnectionScreenEvent.OnBackClicked -> internalState.update {
                it.copy(step = if (it.step == Step.Cable && it.connection.operation is Operation.Identifying) Step.Ecu else Step.Cable)
            }
            is ConnectionScreenEvent.OnAdapterSelected -> connection.selectAdapter(id = event.id)
        }
    }
}
