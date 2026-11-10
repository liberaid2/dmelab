package dev.coldcat.dmelab.common.session.domain

internal enum class ConnectionProblem {
    Permission,
    Disconnected,
    Busy,
    Exchange,
    Protocol,
    NeedsReconnect,
    NativeLibrary,
    Unexpected,
}
