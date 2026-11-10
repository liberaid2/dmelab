package dev.coldcat.dmelab.common.ecu.model

sealed interface UsbInventory {
    data object Scanning : UsbInventory
    data object Unavailable : UsbInventory
    data object Failed : UsbInventory
    data class Available(
        val adapters: List<UsbAdapter>,
        val unsupportedCount: Int,
        val permissionPendingId: String?,
        val permissionDeniedId: String?,
    ) : UsbInventory
}
