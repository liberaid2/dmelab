package dev.coldcat.dmelab.features.firmware.open.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.coldcat.dmelab.common.session.domain.EcuOperations
import dev.coldcat.dmelab.common.session.domain.FirmwareFileException
import dev.coldcat.dmelab.common.session.domain.FirmwareFiles
import dev.coldcat.dmelab.common.session.domain.OperationFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/** Activity-owned preparation only; incoming intents can never start an ECU operation. */
@KoinViewModel
class FirmwareOpenViewModel internal constructor(
    private val files: FirmwareFiles,
    private val operations: EcuOperations,
    mapper: FirmwareOpenScreenMapper,
) : ViewModel() {
    private val internalState = MutableStateFlow(FirmwareOpenInternalState())
    private var lastSource: String? = null
    private var importJob: Job? = null
    internal val state = internalState.map(mapper::getScreenState).stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = mapper.getScreenState(internal = internalState.value),
    )

    internal fun onEvent(event: FirmwareOpenScreenEvent) {
        when (event) {
            is FirmwareOpenScreenEvent.OnOpen -> open(location = event.location, newRequest = event.newRequest)
            FirmwareOpenScreenEvent.OnDismiss -> {
                importJob?.cancel()
                internalState.value = FirmwareOpenInternalState()
            }
            is FirmwareOpenScreenEvent.OnDelivered -> if (internalState.value.image?.location == event.location) {
                internalState.value = FirmwareOpenInternalState()
            }
        }
    }

    fun open(location: String, newRequest: Boolean) {
        // Activity recreation must not re-import an already handled launch intent.
        if (!newRequest && lastSource == location) return
        lastSource = location
        importJob?.cancel()
        if (operations.state.value.active != null) {
            internalState.value = FirmwareOpenInternalState(failure = OperationFailure.Busy)
            return
        }
        internalState.value = FirmwareOpenInternalState(importing = true)
        importJob = viewModelScope.launch {
            try {
                val image = files.importImage(source = location)
                internalState.value = FirmwareOpenInternalState(image = image)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: FirmwareFileException) {
                internalState.value = FirmwareOpenInternalState(failure = exception.problem)
            } catch (_: Exception) {
                internalState.value = FirmwareOpenInternalState(failure = OperationFailure.FileAccess)
            }
        }
    }
}
