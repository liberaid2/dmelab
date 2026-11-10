package dev.coldcat.dmelab.common.ecu.model

/** Decoded ECU values; no native objects cross the module boundary. */
data class Progress(
    val stage: String,
    val completed: ULong,
    val total: ULong,
)
