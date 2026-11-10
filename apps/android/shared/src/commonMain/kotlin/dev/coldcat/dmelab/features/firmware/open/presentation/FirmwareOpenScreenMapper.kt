package dev.coldcat.dmelab.features.firmware.open.presentation

import dev.coldcat.dmelab.common.session.domain.OperationFailure
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.firmware_busy
import dev.coldcat.dmelab.shared.resources.firmware_library_error
import dev.coldcat.dmelab.shared.resources.firmware_open_access
import dev.coldcat.dmelab.shared.resources.firmware_open_invalid
import dev.coldcat.dmelab.shared.resources.firmware_unexpected_error
import org.koin.core.annotation.Factory

@Factory
internal class FirmwareOpenScreenMapper {
    fun getScreenState(internal: FirmwareOpenInternalState) = FirmwareOpenScreenState(
        importing = internal.importing,
        image = internal.image,
        message = when (internal.failure) {
            null -> null
            OperationFailure.InvalidImage -> Res.string.firmware_open_invalid
            OperationFailure.Busy -> Res.string.firmware_busy
            OperationFailure.NativeLibrary -> Res.string.firmware_library_error
            OperationFailure.FileAccess, OperationFailure.Permission -> Res.string.firmware_open_access
            OperationFailure.Disconnected, OperationFailure.NeedsReconnect, OperationFailure.Exchange,
            OperationFailure.LowVoltage, OperationFailure.FirmwareMismatch, OperationFailure.Unexpected,
            OperationFailure.Interrupted -> Res.string.firmware_unexpected_error
        },
    )
}
