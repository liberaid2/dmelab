package dev.coldcat.dmelab.common.ecu.model

data class UsbAdapter(
    val id: String,
    val name: String,
    val vendorId: Int,
    val productId: Int,
    val hasPermission: Boolean,
)
