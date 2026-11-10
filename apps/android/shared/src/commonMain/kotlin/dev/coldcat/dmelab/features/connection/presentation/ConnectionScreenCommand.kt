package dev.coldcat.dmelab.features.connection.presentation

internal sealed interface ConnectionScreenCommand {
    data object OpenLogs : ConnectionScreenCommand
}
