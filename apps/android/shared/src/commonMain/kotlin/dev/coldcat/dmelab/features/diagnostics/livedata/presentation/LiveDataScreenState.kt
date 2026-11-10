package dev.coldcat.dmelab.features.diagnostics.livedata.presentation

import dev.coldcat.dmelab.common.ecu.model.LiveDataGroup
import org.jetbrains.compose.resources.StringResource

internal data class LiveDataScreenState(
    val record: Boolean = false,
    val groups: List<Group> = emptyList(),
    val canChangeGroups: Boolean = true,
    val emptyGroupSelection: Boolean = false,
    val showAllLogs: Boolean = false,
    val logCount: String? = null,
    val exportableLogCount: String = "0",
    val canExportAll: Boolean = false,
    val fileBusy: Boolean = false,
    val fileMessage: StringResource? = null,
    val deleteId: String? = null,
    val rename: Rename? = null,
    val logs: List<Log> = emptyList(),
    val selectedLogCount: String? = null,
    val canShareSelected: Boolean = false,
    val canStart: Boolean = false,
    val canStop: Boolean = false,
    val polling: Boolean = false,
    val stopping: Boolean = false,
    val activeOperation: StringResource? = null,
    val connectionRequired: Boolean = false,
    val error: StringResource? = null,
    val updatedAt: String? = null,
    val readDuration: String? = null,
    val sampleInterval: String? = null,
    val recoveryCount: String? = null,
    val updateFrequency: String? = null,
    val values: List<Value>? = null,
) {
    data class Group(val id: LiveDataGroup, val title: StringResource, val description: StringResource, val selected: Boolean)
    data class Rename(val name: String, val suffix: String, val canConfirm: Boolean,
        val busy: Boolean = false, val error: StringResource? = null)
    data class Log(val id: String, val samples: String, val size: String, val duration: String,
        val recording: Boolean = false, val partial: Boolean = false, val available: Boolean = true,
        val selected: Boolean = false)
    data class Value(val label: String, val value: String?, val unit: String, val flag: Boolean?)
}
