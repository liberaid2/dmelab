package dev.coldcat.dmelab.common.platform

import org.jetbrains.compose.resources.StringResource

internal data class LogSharingLabels(
    val action: StringResource,
    val archiveAction: StringResource,
    val selectionHint: StringResource,
    val selectionActive: StringResource,
)

internal expect fun logSharingLabels(): LogSharingLabels
