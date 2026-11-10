package dev.coldcat.dmelab.common.session.presentation

import dev.coldcat.dmelab.common.session.domain.OperationFailure
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.operation_failure_busy
import dev.coldcat.dmelab.shared.resources.operation_failure_disconnected
import dev.coldcat.dmelab.shared.resources.operation_failure_exchange
import dev.coldcat.dmelab.shared.resources.operation_failure_fileaccess
import dev.coldcat.dmelab.shared.resources.operation_failure_firmwaremismatch
import dev.coldcat.dmelab.shared.resources.operation_failure_interrupted
import dev.coldcat.dmelab.shared.resources.operation_failure_invalidimage
import dev.coldcat.dmelab.shared.resources.operation_failure_lowvoltage
import dev.coldcat.dmelab.shared.resources.operation_failure_nativelibrary
import dev.coldcat.dmelab.shared.resources.operation_failure_needsreconnect
import dev.coldcat.dmelab.shared.resources.operation_failure_permission
import dev.coldcat.dmelab.shared.resources.operation_failure_unexpected
import org.jetbrains.compose.resources.StringResource

internal fun OperationFailure.messageResource(): StringResource = when (this) {
    OperationFailure.Busy -> Res.string.operation_failure_busy
    OperationFailure.Disconnected -> Res.string.operation_failure_disconnected
    OperationFailure.Permission -> Res.string.operation_failure_permission
    OperationFailure.NeedsReconnect -> Res.string.operation_failure_needsreconnect
    OperationFailure.Exchange -> Res.string.operation_failure_exchange
    OperationFailure.InvalidImage -> Res.string.operation_failure_invalidimage
    OperationFailure.LowVoltage -> Res.string.operation_failure_lowvoltage
    OperationFailure.FirmwareMismatch -> Res.string.operation_failure_firmwaremismatch
    OperationFailure.FileAccess -> Res.string.operation_failure_fileaccess
    OperationFailure.NativeLibrary -> Res.string.operation_failure_nativelibrary
    OperationFailure.Unexpected -> Res.string.operation_failure_unexpected
    OperationFailure.Interrupted -> Res.string.operation_failure_interrupted
}
