package dev.coldcat.dmelab.features.diagnostics.livedata.presentation

import dev.coldcat.dmelab.common.ecu.model.LiveDataSelection
import dev.coldcat.dmelab.common.session.domain.EcuConnectionState
import dev.coldcat.dmelab.common.session.domain.EcuOperationState
import dev.coldcat.dmelab.common.session.domain.LiveDataLog
import org.jetbrains.compose.resources.StringResource

internal data class LiveDataInternalState(
    val connection: EcuConnectionState,
    val operations: EcuOperationState,
    val logs: List<LiveDataLog> = emptyList(),
    val selectedLogIds: Set<String> = emptySet(),
    val record: Boolean = false,
    val selection: LiveDataSelection = LiveDataSelection(),
    val showAllLogs: Boolean = false,
    val fileBusy: Boolean = false,
    val fileMessage: StringResource? = null,
    val exportTarget: LiveDataExportTarget? = null,
    val deleteId: String? = null,
    val rename: Rename? = null,
) {
    data class Rename(val id: String, val name: String, val partial: Boolean, val failure: StringResource? = null)
}
