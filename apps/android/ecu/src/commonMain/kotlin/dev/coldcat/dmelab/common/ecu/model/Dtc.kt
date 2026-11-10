package dev.coldcat.dmelab.common.ecu.model

/** Decoded ECU values; no native objects cross the module boundary. */
data class Dtc(
    val code: UByte,
    val frequency: UByte,
    val data: ByteArray,
    val raw: ByteArray,
)
