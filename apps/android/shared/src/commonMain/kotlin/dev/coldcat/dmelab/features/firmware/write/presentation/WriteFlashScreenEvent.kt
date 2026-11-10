package dev.coldcat.dmelab.features.firmware.write.presentation

import dev.coldcat.dmelab.common.ecu.model.FirmwareMode

internal sealed interface WriteFlashScreenEvent {
    data object OnBack : WriteFlashScreenEvent
    data class OnMode(val mode: FirmwareMode) : WriteFlashScreenEvent
    data object OnChooseFile : WriteFlashScreenEvent
    data class OnFile(val location: String) : WriteFlashScreenEvent
    data class OnExternalFile(val location: String, val mode: FirmwareMode) : WriteFlashScreenEvent
    data object OnRequestConfirmation : WriteFlashScreenEvent
    data object OnDismissConfirmation : WriteFlashScreenEvent
    data object OnConfirm : WriteFlashScreenEvent
    data object OnFileAccessFailure : WriteFlashScreenEvent
}
