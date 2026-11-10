package dev.coldcat.dmelab.features.diagnostics.dtc.presentation

import org.jetbrains.compose.resources.StringResource

internal data class DtcScreenState(
    val canRead: Boolean = false,
    val canClear: Boolean = false,
    val confirmationVisible: Boolean = false,
    val activeOperation: StringResource? = null,
    val connectionRequired: Boolean = false,
    val error: StringResource? = null,
    val updatedAt: String? = null,
    val clearAcknowledgedAt: String? = null,
    val entries: List<Entry>? = null,
) {
    data class Entry(val code: String, val frequency: String, val data: String, val raw: String)
}
