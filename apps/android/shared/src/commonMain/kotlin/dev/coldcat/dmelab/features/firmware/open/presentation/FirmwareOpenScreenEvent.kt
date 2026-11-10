package dev.coldcat.dmelab.features.firmware.open.presentation

internal sealed interface FirmwareOpenScreenEvent {
    data class OnOpen(val location: String, val newRequest: Boolean = false) : FirmwareOpenScreenEvent
    data object OnDismiss : FirmwareOpenScreenEvent
    data class OnDelivered(val location: String) : FirmwareOpenScreenEvent
}
