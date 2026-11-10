package dev.coldcat.dmelab.common.ecu.session

/** Main-safe, exclusive access; the client is valid only for the operation's lifetime. */
interface Ms43Access {
    suspend fun <T> use(adapterId: String, operation: suspend (EcuClient) -> T): T
}
