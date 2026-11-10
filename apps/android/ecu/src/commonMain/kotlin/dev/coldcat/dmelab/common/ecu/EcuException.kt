package dev.coldcat.dmelab.common.ecu

/** Stable failure categories; the native cause is retained for diagnostics. */
class EcuException internal constructor(val kind: Kind, cause: Throwable) : Exception(cause) {
    enum class Kind { Busy, NeedsReconnect, InvalidImage, LowVoltage, FirmwareMismatch, Protocol, Exchange, Permission, NativeLibrary }
}
