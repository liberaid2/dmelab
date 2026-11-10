package dev.coldcat.dmelab.common.ecu.model

/** Decoded ECU values; no native objects cross the module boundary. */
data class VanosPosition(
    val actualDeg: Double,
    val targetDeg: Double,
    val referenceDeg: Double,
)
