package dev.coldcat.dmelab.features.connection.presentation

import org.jetbrains.compose.resources.StringResource

internal data class ConnectionInstructions(
    val cableTitle: StringResource,
    val cableBody: StringResource,
    val waitingBody: StringResource,
    val chooseBody: StringResource,
    val footer: StringResource,
)

internal expect fun connectionInstructions(): ConnectionInstructions
