package dev.coldcat.dmelab.common.ecu.model

/** Decoded ECU values; no native objects cross the module boundary. */
data class LiveVanos(
    val intake: VanosPosition,
    val exhaust: VanosPosition,
)
