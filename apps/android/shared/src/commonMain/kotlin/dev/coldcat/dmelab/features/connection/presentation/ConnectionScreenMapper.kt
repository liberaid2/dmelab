package dev.coldcat.dmelab.features.connection.presentation

import dev.coldcat.dmelab.common.ecu.model.UsbInventory
import dev.coldcat.dmelab.common.session.domain.ConnectionProblem
import dev.coldcat.dmelab.common.session.domain.EcuConnectionState.Operation
import dev.coldcat.dmelab.features.connection.presentation.ConnectionScreenState.Notice
import dev.coldcat.dmelab.features.connection.presentation.ConnectionScreenState.PrimaryAction
import dev.coldcat.dmelab.features.connection.presentation.ConnectionScreenState.Step
import dev.coldcat.dmelab.features.connection.presentation.ConnectionScreenState.Tone
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.cable_description
import dev.coldcat.dmelab.shared.resources.cable_title
import dev.coldcat.dmelab.shared.resources.continue_button
import dev.coldcat.dmelab.shared.resources.ecu_busy
import dev.coldcat.dmelab.shared.resources.ecu_busy_body
import dev.coldcat.dmelab.shared.resources.ecu_check_failed
import dev.coldcat.dmelab.shared.resources.ecu_check_failed_body
import dev.coldcat.dmelab.shared.resources.ecu_connect_button
import dev.coldcat.dmelab.shared.resources.ecu_description
import dev.coldcat.dmelab.shared.resources.ecu_exchange_failed
import dev.coldcat.dmelab.shared.resources.ecu_exchange_failed_body
import dev.coldcat.dmelab.shared.resources.ecu_identified
import dev.coldcat.dmelab.shared.resources.ecu_identified_body
import dev.coldcat.dmelab.shared.resources.ecu_identify_again
import dev.coldcat.dmelab.shared.resources.ecu_identifying
import dev.coldcat.dmelab.shared.resources.ecu_identifying_body
import dev.coldcat.dmelab.shared.resources.ecu_identifying_button
import dev.coldcat.dmelab.shared.resources.ecu_library_failed
import dev.coldcat.dmelab.shared.resources.ecu_library_failed_body
import dev.coldcat.dmelab.shared.resources.ecu_protocol_failed
import dev.coldcat.dmelab.shared.resources.ecu_protocol_failed_body
import dev.coldcat.dmelab.shared.resources.ecu_ready_body
import dev.coldcat.dmelab.shared.resources.ecu_recovery_body
import dev.coldcat.dmelab.shared.resources.ecu_recovery_required
import dev.coldcat.dmelab.shared.resources.ecu_success_description
import dev.coldcat.dmelab.shared.resources.ecu_success_title
import dev.coldcat.dmelab.shared.resources.ecu_title
import dev.coldcat.dmelab.shared.resources.open_identification
import dev.coldcat.dmelab.shared.resources.usb_allow
import dev.coldcat.dmelab.shared.resources.usb_choose
import dev.coldcat.dmelab.shared.resources.usb_disconnected
import dev.coldcat.dmelab.shared.resources.usb_disconnected_busy
import dev.coldcat.dmelab.shared.resources.usb_permission_body
import dev.coldcat.dmelab.shared.resources.usb_permission_denied
import dev.coldcat.dmelab.shared.resources.usb_permission_needed
import dev.coldcat.dmelab.shared.resources.usb_permission_pending
import dev.coldcat.dmelab.shared.resources.usb_permission_pending_body
import dev.coldcat.dmelab.shared.resources.usb_ready
import dev.coldcat.dmelab.shared.resources.usb_ready_body
import dev.coldcat.dmelab.shared.resources.usb_refresh
import dev.coldcat.dmelab.shared.resources.usb_scan_failed
import dev.coldcat.dmelab.shared.resources.usb_scan_failed_body
import dev.coldcat.dmelab.shared.resources.usb_scanning
import dev.coldcat.dmelab.shared.resources.usb_scanning_body
import dev.coldcat.dmelab.shared.resources.usb_unavailable
import dev.coldcat.dmelab.shared.resources.usb_unavailable_body
import dev.coldcat.dmelab.shared.resources.usb_unsupported
import dev.coldcat.dmelab.shared.resources.usb_unsupported_body
import dev.coldcat.dmelab.shared.resources.usb_waiting
import dev.coldcat.dmelab.shared.resources.usb_waiting_button
import org.koin.core.annotation.Factory

@Factory
internal class ConnectionScreenMapper {
    fun getScreenState(internal: ConnectionInternalState): ConnectionScreenState {
        val connection = internal.connection
        val inventory = connection.inventory
        val available = inventory as? UsbInventory.Available
        val selected = available?.adapters?.firstOrNull { it.id == connection.selectedId }
        val busy = connection.operation is Operation.Identifying
        val permissionPending = selected != null && available.permissionPendingId == selected.id
        val identified = (connection.operation as? Operation.Identified)?.information
        val failure = (connection.operation as? Operation.Failed)?.problem
        val notice = when {
            inventory is UsbInventory.Scanning -> Notice(Res.string.usb_scanning, Res.string.usb_scanning_body, Tone.Neutral)
            inventory is UsbInventory.Unavailable -> Notice(Res.string.usb_unavailable, Res.string.usb_unavailable_body, Tone.Error)
            inventory is UsbInventory.Failed -> Notice(Res.string.usb_scan_failed, Res.string.usb_scan_failed_body, Tone.Error)
            selected == null && busy -> Notice(Res.string.usb_disconnected, Res.string.usb_disconnected_busy, Tone.Error)
            selected == null && available?.adapters?.isNotEmpty() == true -> Notice(Res.string.usb_choose, connectionInstructions().chooseBody, Tone.Neutral)
            selected == null && (available?.unsupportedCount ?: 0) > 0 -> Notice(Res.string.usb_unsupported, Res.string.usb_unsupported_body, Tone.Attention)
            selected == null -> Notice(Res.string.usb_waiting, connectionInstructions().waitingBody, Tone.Neutral)
            permissionPending -> Notice(Res.string.usb_permission_pending, Res.string.usb_permission_pending_body, Tone.Attention)
            !selected.hasPermission -> Notice(
                title = if (available.permissionDeniedId == selected.id) Res.string.usb_permission_denied else Res.string.usb_permission_needed,
                description = Res.string.usb_permission_body,
                tone = Tone.Attention,
            )
            busy -> Notice(Res.string.ecu_identifying, Res.string.ecu_identifying_body, Tone.Neutral)
            failure != null -> failureNotice(problem = failure)
            identified != null -> Notice(Res.string.ecu_identified, Res.string.ecu_identified_body, Tone.Success)
            else -> Notice(Res.string.usb_ready, if (internal.step == Step.Cable) Res.string.usb_ready_body else Res.string.ecu_ready_body, Tone.Success)
        }
        val primary = when {
            internal.step == Step.Cable && busy -> PrimaryAction(Res.string.open_identification, ConnectionScreenEvent.OnContinueClicked, false)
            busy -> PrimaryAction(Res.string.ecu_identifying_button, null, true)
            permissionPending -> PrimaryAction(Res.string.usb_permission_pending, null, true)
            selected != null && !selected.hasPermission -> PrimaryAction(Res.string.usb_allow, ConnectionScreenEvent.OnPermissionClicked, false)
            inventory is UsbInventory.Failed || inventory is UsbInventory.Unavailable -> PrimaryAction(Res.string.usb_refresh, ConnectionScreenEvent.OnRefreshClicked, false)
            selected == null -> PrimaryAction(if (available?.adapters?.isNotEmpty() == true) Res.string.usb_choose else Res.string.usb_waiting_button, null, false)
            internal.step == Step.Cable -> PrimaryAction(Res.string.continue_button, ConnectionScreenEvent.OnContinueClicked, false)
            failure == ConnectionProblem.NeedsReconnect -> PrimaryAction(Res.string.ecu_recovery_required, null, false)
            identified != null -> PrimaryAction(Res.string.ecu_identify_again, ConnectionScreenEvent.OnIdentifyClicked, false)
            else -> PrimaryAction(Res.string.ecu_connect_button, ConnectionScreenEvent.OnIdentifyClicked, false)
        }
        return ConnectionScreenState(
            step = internal.step,
            title = when (internal.step) {
                Step.Cable -> Res.string.cable_title
                Step.Ecu -> if (identified != null) Res.string.ecu_success_title else Res.string.ecu_title
            },
            description = when (internal.step) {
                Step.Cable -> Res.string.cable_description
                Step.Ecu -> if (identified != null) Res.string.ecu_success_description else Res.string.ecu_description
            },
            adapters = available?.adapters.orEmpty().map { adapter ->
                ConnectionScreenState.AdapterItem(
                    id = adapter.id,
                    name = adapter.name,
                    description = "USB ${adapter.vendorId.toString(16).uppercase().padStart(4, '0')}:" +
                        "${adapter.productId.toString(16).uppercase().padStart(4, '0')} · ${adapter.id.substringAfterLast('/')}",
                )
            },
            selectedId = selected?.id,
            notice = notice,
            primary = primary,
            information = identified?.let {
                ConnectionScreenState.EcuCard(hardwareId = it.hardwareId, flashIdentifier = it.flashIdentifier, firmwareVersion = it.firmwareVersion)
            },
            isBusy = busy,
        )
    }

    private fun failureNotice(problem: ConnectionProblem): Notice = when (problem) {
        ConnectionProblem.Permission -> Notice(Res.string.usb_permission_needed, Res.string.usb_permission_body, Tone.Attention)
        ConnectionProblem.Disconnected -> Notice(Res.string.usb_disconnected, connectionInstructions().waitingBody, Tone.Error)
        ConnectionProblem.Busy -> Notice(Res.string.ecu_busy, Res.string.ecu_busy_body, Tone.Attention)
        ConnectionProblem.Exchange -> Notice(Res.string.ecu_exchange_failed, Res.string.ecu_exchange_failed_body, Tone.Error)
        ConnectionProblem.Protocol -> Notice(Res.string.ecu_protocol_failed, Res.string.ecu_protocol_failed_body, Tone.Error)
        ConnectionProblem.NeedsReconnect -> Notice(Res.string.ecu_recovery_required, Res.string.ecu_recovery_body, Tone.Error)
        ConnectionProblem.NativeLibrary -> Notice(Res.string.ecu_library_failed, Res.string.ecu_library_failed_body, Tone.Error)
        ConnectionProblem.Unexpected -> Notice(Res.string.ecu_check_failed, Res.string.ecu_check_failed_body, Tone.Error)
    }
}
