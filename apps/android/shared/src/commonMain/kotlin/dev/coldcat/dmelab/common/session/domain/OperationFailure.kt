package dev.coldcat.dmelab.common.session.domain

enum class OperationFailure { Busy, Disconnected, Permission, NeedsReconnect, Exchange, InvalidImage, LowVoltage, FirmwareMismatch, FileAccess, NativeLibrary, Unexpected, Interrupted }
