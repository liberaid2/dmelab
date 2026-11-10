package dev.coldcat.dmelab.features.firmware.read.presentation

import dev.coldcat.dmelab.common.ecu.model.FirmwareMode

internal sealed interface ReadFlashScreenEvent {
    data object OnBack : ReadFlashScreenEvent
    data class OnMode(val mode: FirmwareMode) : ReadFlashScreenEvent
    data object OnChooseDestination : ReadFlashScreenEvent
    data class OnDestination(val location: String) : ReadFlashScreenEvent
    data object OnFileAccessFailure : ReadFlashScreenEvent
}
