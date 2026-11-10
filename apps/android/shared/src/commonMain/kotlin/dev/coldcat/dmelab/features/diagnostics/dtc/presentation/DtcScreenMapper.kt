package dev.coldcat.dmelab.features.diagnostics.dtc.presentation

import dev.coldcat.dmelab.common.ecu.model.UsbInventory
import dev.coldcat.dmelab.common.session.domain.DiagnosticResult
import dev.coldcat.dmelab.common.session.domain.EcuConnectionState.Operation
import dev.coldcat.dmelab.common.session.presentation.messageResource
import dev.coldcat.dmelab.features.diagnostics.presentation.DiagnosticMessages
import org.koin.core.annotation.Factory

@Factory
internal class DtcScreenMapper {
    fun getScreenState(internal: DtcInternalState): DtcScreenState {
        val operation = if (internal.connection.operation is Operation.Identified &&
            internal.operations.resultSessionId != internal.connection.sessionId
        ) {
            internal.operations.copy(
                dtcs = DiagnosticResult(),
                dtcClear = DiagnosticResult(),
            )
        } else internal.operations
        val ready = internal.connection.operation is Operation.Identified &&
            (internal.connection.inventory as? UsbInventory.Available)?.adapters?.any {
                it.id == internal.connection.selectedId && it.hasPermission
            } == true
        val idle = operation.active == null
        return DtcScreenState(
            canRead = ready && idle,
            connectionRequired = !ready,
            canClear = ready && idle,
            confirmationVisible = internal.confirmingSessionId == internal.connection.sessionId,
            activeOperation = operation.active?.kind?.let(DiagnosticMessages::operation),
            error = (operation.dtcClear.failure ?: operation.dtcs.failure)?.messageResource(),
            updatedAt = operation.dtcs.updatedAt?.let(DiagnosticMessages::time),
            clearAcknowledgedAt = operation.dtcClear.updatedAt
                ?.takeIf { it > (operation.dtcs.updatedAt ?: Long.MIN_VALUE) }
                ?.let(DiagnosticMessages::time),
            entries = operation.dtcs.value?.map {
                DtcScreenState.Entry(
                    code = "${it.code} · 0x${it.code.toString(16).uppercase().padStart(2, '0')}",
                    frequency = it.frequency.toString(),
                    data = it.data,
                    raw = it.raw,
                )
            },
        )
    }
}
