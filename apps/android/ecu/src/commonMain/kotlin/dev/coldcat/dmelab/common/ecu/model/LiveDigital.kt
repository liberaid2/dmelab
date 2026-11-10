package dev.coldcat.dmelab.common.ecu.model

/** Decoded ECU values; no native objects cross the module boundary. */
data class LiveDigital(
    val disaCommandActive: Boolean,
)
