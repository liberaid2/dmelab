package dev.coldcat.dmelab.features.hub.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.coldcat.dmelab.common.navigation.Destination
import dev.coldcat.dmelab.common.session.domain.EcuConnection
import dev.coldcat.dmelab.common.session.domain.EcuConnectionState.Operation
import dev.coldcat.dmelab.common.session.domain.EcuOperations
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
internal class HubViewModel(
    private val connection: EcuConnection,
    operations: EcuOperations,
    mapper: HubScreenMapper,
) : ViewModel() {
    private val commandChannel = Channel<HubScreenCommand>(capacity = Channel.BUFFERED)
    val commands = commandChannel.receiveAsFlow()
    private val internalState = MutableStateFlow(HubInternalState(connection = connection.state.value, busy = operations.state.value.active != null))
    val state = internalState.map(mapper::getScreenState).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = mapper.getScreenState(internal = internalState.value),
    )

    init {
        viewModelScope.launch {
            operations.state.collect { current ->
                internalState.update { it.copy(busy = current.active != null) }
            }
        }
        viewModelScope.launch {
            connection.state.collect { current ->
                internalState.update {
                    it.copy(connection = current)
                }
            }
        }
    }

    fun onEvent(event: HubScreenEvent) {
        when (event) {
            is HubScreenEvent.OnScenarioSelected -> if (connection.state.value.operation is Operation.Identified) {
                val destination = when (event.scenario) {
                    HubScenario.ReadFlash -> Destination.ReadFlash
                    HubScenario.WriteFlash -> Destination.WriteFlash
                    HubScenario.ReadDtc -> Destination.Dtc
                    HubScenario.LiveData -> Destination.LiveData
                }
                commandChannel.trySend(HubScreenCommand.Open(destination = destination))
            }
            HubScreenEvent.OnChangeConnectionClicked -> connection.clearIdentification()
        }
    }
}
