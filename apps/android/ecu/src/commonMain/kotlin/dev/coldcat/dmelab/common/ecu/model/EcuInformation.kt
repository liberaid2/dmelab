package dev.coldcat.dmelab.common.ecu.model

data class EcuInformation(
    val hardwareId: String,
    val flashIdentifier: String,
    val firmwareVersion: String,
)
