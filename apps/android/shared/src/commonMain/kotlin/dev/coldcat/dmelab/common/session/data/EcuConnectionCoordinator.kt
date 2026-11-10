package dev.coldcat.dmelab.common.session.data

import dev.coldcat.dmelab.common.session.domain.DiagnosticLogger
import dev.coldcat.dmelab.common.ecu.EcuException
import dev.coldcat.dmelab.common.ecu.model.UsbInventory
import dev.coldcat.dmelab.common.ecu.session.IdentifyEcu
import dev.coldcat.dmelab.common.ecu.usb.UsbAdapterMonitor
import dev.coldcat.dmelab.common.session.domain.ConnectionProblem
import dev.coldcat.dmelab.common.session.domain.EcuConnection
import dev.coldcat.dmelab.common.session.domain.EcuConnectionState
import dev.coldcat.dmelab.common.session.domain.EcuConnectionState.Operation
import dev.coldcat.dmelab.common.session.domain.EcuSessionLease
import kotlinx.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/** Main-thread coordinator shared across screens; identification survives ViewModel recreation. */
@Single
internal class EcuConnectionCoordinator(
    private val logger: DiagnosticLogger,
    private val usb: UsbAdapterMonitor,
    private val identifyEcu: IdentifyEcu,
    @Named("application") private val scope: CoroutineScope,
    @Named("ecu") private val ecuDispatcher: CoroutineDispatcher,
) : EcuConnection {
    private val mutableState = MutableStateFlow(
        EcuConnectionState(inventory = UsbInventory.Scanning, selectedId = null, operation = Operation.Idle),
    )
    override val state = mutableState.asStateFlow()
    private var operationJob: Job? = null
    private var generation = 0L
    private var reserved: EcuSessionLease? = null

    init {
        scope.launch {
            usb.inventory.collect { inventory ->
                val current = mutableState.value
                val adapters = (inventory as? UsbInventory.Available)?.adapters.orEmpty()
                val selected = adapters.firstOrNull { it.id == current.selectedId }
                    ?: adapters.singleOrNull()
                val changed = selected?.id != current.selectedId ||
                    (selected != null && !selected.hasPermission && current.operation is Operation.Identified)
                if (changed) generation++
                mutableState.update {
                    it.copy(
                        inventory = inventory,
                        selectedId = selected?.id,
                        sessionId = generation,
                        operation = if (changed && it.operation !is Operation.Identifying) Operation.Idle else it.operation,
                    )
                }
                if (selected != null && !selected.hasPermission) {
                    usb.requestPermission(id = selected.id, force = false)
                }
            }
        }
    }

    override fun refresh() = usb.refresh()

    override fun clearIdentification() {
        if (operationJob?.isActive == true || reserved != null) return
        generation++
        mutableState.update { it.copy(operation = Operation.Idle, sessionId = generation) }
    }

    override fun tryReserve(): EcuSessionLease? {
        if (operationJob?.isActive == true || reserved != null) return null
        val current = mutableState.value
        val information = (current.operation as? Operation.Identified)?.information ?: return null
        val adapter = (current.inventory as? UsbInventory.Available)?.adapters
            ?.firstOrNull { it.id == current.selectedId && it.hasPermission } ?: return null
        return EcuSessionLease(adapterId = adapter.id, sessionId = current.sessionId, information = information)
            .also { reserved = it }
    }

    override fun release(lease: EcuSessionLease) {
        if (reserved == lease) reserved = null
    }

    override fun invalidate(problem: ConnectionProblem) {
        generation++
        mutableState.update { it.copy(operation = Operation.Failed(problem = problem), sessionId = generation) }
    }

    override fun selectAdapter(id: String) {
        if (operationJob?.isActive == true || reserved != null) return
        val inventory = mutableState.value.inventory as? UsbInventory.Available ?: return
        val adapter = inventory.adapters.firstOrNull { it.id == id } ?: return
        generation++
        mutableState.update { it.copy(selectedId = id, operation = Operation.Idle, sessionId = generation) }
        if (!adapter.hasPermission) usb.requestPermission(id = id, force = false)
    }

    override fun requestPermission() {
        if (operationJob?.isActive == true || reserved != null) return
        mutableState.value.selectedId?.let { usb.requestPermission(id = it, force = true) }
    }

    override fun identify() {
        if (operationJob?.isActive == true || reserved != null) return
        val current = mutableState.value
        if ((current.operation as? Operation.Failed)?.problem == ConnectionProblem.NeedsReconnect) return
        val inventory = current.inventory as? UsbInventory.Available ?: return
        val adapter = inventory.adapters.firstOrNull { it.id == current.selectedId && it.hasPermission } ?: return
        val startedGeneration = generation
        mutableState.update { it.copy(operation = Operation.Identifying) }
        operationJob = scope.launch {
            val result = try {
                val information = withContext(context = ecuDispatcher) { identifyEcu.invoke(adapterId = adapter.id) }
                Operation.Identified(information = information)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                logger.error(message = "Identification failed: ${exception::class.simpleName}")
                Operation.Failed(problem = problemFor(exception = exception))
            }
            usb.refresh()
            val stillAvailable = (usb.inventory.value as? UsbInventory.Available)?.adapters
                ?.any { it.id == adapter.id && it.hasPermission } == true
            val accept = startedGeneration == generation && stillAvailable
            if (accept) generation++
            mutableState.update {
                it.copy(operation = if (accept) result else Operation.Idle, sessionId = generation)
            }
        }
    }

    private fun problemFor(exception: Exception): ConnectionProblem = when (exception) {
        is IOException -> ConnectionProblem.Exchange
        is EcuException -> when (exception.kind) {
            EcuException.Kind.Permission -> ConnectionProblem.Permission
            EcuException.Kind.NativeLibrary -> ConnectionProblem.NativeLibrary
            EcuException.Kind.Busy -> ConnectionProblem.Busy
            EcuException.Kind.NeedsReconnect -> ConnectionProblem.NeedsReconnect
            EcuException.Kind.Protocol -> ConnectionProblem.Protocol
            EcuException.Kind.Exchange -> ConnectionProblem.Exchange
            EcuException.Kind.InvalidImage, EcuException.Kind.LowVoltage, EcuException.Kind.FirmwareMismatch -> ConnectionProblem.Unexpected
        }
        else -> ConnectionProblem.Unexpected
    }
}
