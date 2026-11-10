package dev.coldcat.dmelab.common.session.domain

import dev.coldcat.dmelab.common.ecu.model.FirmwareMode

interface FirmwareFiles {
    suspend fun importImage(source: String): FirmwareImage
    suspend fun inspect(source: String, mode: FirmwareMode): FirmwareImage
    suspend fun read(source: String): ByteArray
    fun fingerprint(bytes: ByteArray): String

    /** Open the destination before exchange; clean up an incomplete document on failure. */
    suspend fun save(destination: String, readImage: suspend () -> ByteArray)
}
