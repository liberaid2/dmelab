package dev.coldcat.dmelab.common.ecu

/** Main-safe metadata access independent of the platform's native loader. */
interface EcuLibrary {
    suspend fun version(): String
}
