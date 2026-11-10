package dev.coldcat.dmelab.features.diagnostics.livedata.presentation

import dev.coldcat.dmelab.common.ecu.model.LiveDataGroup
import dev.coldcat.dmelab.common.ecu.model.UsbInventory
import dev.coldcat.dmelab.common.presentation.formatDecimal
import dev.coldcat.dmelab.common.session.domain.EcuConnectionState.Operation
import dev.coldcat.dmelab.common.session.domain.LiveDataLogName
import dev.coldcat.dmelab.common.session.domain.OperationKind
import dev.coldcat.dmelab.common.session.presentation.formatOperationDuration
import dev.coldcat.dmelab.common.session.presentation.messageResource
import dev.coldcat.dmelab.features.diagnostics.presentation.DiagnosticMessages
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.live_group_digital
import dev.coldcat.dmelab.shared.resources.live_group_digital_description
import dev.coldcat.dmelab.shared.resources.live_group_measurements
import dev.coldcat.dmelab.shared.resources.live_group_measurements_description
import dev.coldcat.dmelab.shared.resources.live_group_vanos
import dev.coldcat.dmelab.shared.resources.live_group_vanos_description
import dev.coldcat.dmelab.shared.resources.live_log_invalid_name
import org.koin.core.annotation.Factory

@Factory
internal class LiveDataScreenMapper {
    fun getScreenState(internal: LiveDataInternalState): LiveDataScreenState {
        val result = internal.operations.liveData.takeUnless {
            internal.connection.operation is Operation.Identified &&
                internal.operations.resultSessionId != internal.connection.sessionId
        }
        val active = internal.operations.active
        val pollingOperation = active?.takeIf { it.kind == OperationKind.LiveData && it.polling }
        val ready = internal.connection.operation is Operation.Identified &&
            (internal.connection.inventory as? UsbInventory.Available)?.adapters?.any {
                it.id == internal.connection.selectedId && it.hasPermission
            } == true
        val exportableCount = internal.logs.count { !it.recording && it.available }
        val timing = internal.operations.liveDataTiming.takeIf { result != null }
        val selection = active?.liveDataSelection ?: internal.selection
        return LiveDataScreenState(
            groups = listOf(
                LiveDataScreenState.Group(LiveDataGroup.Measurements, Res.string.live_group_measurements,
                    Res.string.live_group_measurements_description, selection.measurements),
                LiveDataScreenState.Group(LiveDataGroup.Vanos, Res.string.live_group_vanos,
                    Res.string.live_group_vanos_description, selection.vanos),
                LiveDataScreenState.Group(LiveDataGroup.Digital, Res.string.live_group_digital,
                    Res.string.live_group_digital_description, selection.digital),
            ),
            canChangeGroups = active == null,
            emptyGroupSelection = !selection.isNotEmpty,
            record = if (pollingOperation != null) internal.logs.any { it.recording } else internal.record,
            showAllLogs = internal.showAllLogs,
            logCount = internal.logs.size.takeIf { it > 1 }?.toString(),
            exportableLogCount = exportableCount.toString(),
            canExportAll = exportableCount > 0 && !internal.fileBusy,
            fileBusy = internal.fileBusy,
            fileMessage = internal.fileMessage,
            deleteId = internal.deleteId,
            rename = internal.rename?.let { rename ->
                val valid = LiveDataLogName.stem(input = rename.name, partial = rename.partial) != null
                LiveDataScreenState.Rename(name = rename.name, suffix = if (rename.partial) "_partial.csv" else ".csv",
                    canConfirm = valid && !internal.fileBusy, busy = internal.fileBusy,
                    error = if (valid) rename.failure else Res.string.live_log_invalid_name)
            },
            selectedLogCount = internal.selectedLogIds.size.takeIf { it > 0 }?.toString(),
            canShareSelected = internal.selectedLogIds.isNotEmpty() && !internal.fileBusy,
            logs = (if (internal.showAllLogs) internal.logs else internal.logs.take(1)).map { log ->
                LiveDataScreenState.Log(id = log.id, samples = log.samples.toString(),
                    size = formatDecimal(value = log.byteCount / 1024.0, decimals = 1, fixed = true),
                    duration = formatOperationDuration(elapsedMillis = log.elapsedMillis),
                    recording = log.recording, partial = log.partial, available = log.available,
                    selected = log.id in internal.selectedLogIds)
            },
            canStart = ready && active == null && selection.isNotEmpty,
            canStop = pollingOperation != null && !pollingOperation.stopping,
            polling = pollingOperation != null,
            stopping = pollingOperation?.stopping == true,
            connectionRequired = !ready,
            activeOperation = active?.kind?.let(DiagnosticMessages::operation),
            error = result?.failure?.messageResource(),
            updatedAt = result?.updatedAt?.let(DiagnosticMessages::time),
            readDuration = timing?.readDurationMillis?.toString(),
            sampleInterval = timing?.sampleIntervalMillis?.toString(),
            recoveryCount = internal.operations.liveDataRecoveryCount
                ?.takeIf { pollingOperation != null && !pollingOperation.stopping }?.toString(),
            updateFrequency = timing?.updateFrequencyHz?.let { formatDecimal(value = it, fixed = true) },
            values = result?.value?.map {
                LiveDataScreenState.Value(
                    label = it.label,
                    value = it.value?.let { value -> formatDecimal(value = value) },
                    unit = it.unit,
                    flag = it.flag,
                )
            },
        )
    }
}
