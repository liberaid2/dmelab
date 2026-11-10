package dev.coldcat.dmelab.common.ecu.session

import dev.coldcat.dmelab.common.ecu.model.FirmwareMode

/** Main-safe validation without reserving the ECU session. */
interface FirmwareValidator {
    suspend fun validateImage(image: ByteArray, mode: FirmwareMode)
}
