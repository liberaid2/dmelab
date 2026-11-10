package dev.coldcat.dmelab.features.diagnostics.livedata.presentation
import dev.coldcat.dmelab.common.ecu.model.LiveDataGroup

internal sealed interface LiveDataScreenEvent {
    data class OnGroupChanged(val group: LiveDataGroup, val enabled: Boolean) : LiveDataScreenEvent
    data class OnRecordChanged(val enabled: Boolean) : LiveDataScreenEvent
    data class OnShare(val id: String) : LiveDataScreenEvent
    data class OnToggleSelection(val id: String) : LiveDataScreenEvent
    data object OnShareSelected : LiveDataScreenEvent
    data class OnSave(val id: String) : LiveDataScreenEvent
    data class OnDestination(val uri: String?) : LiveDataScreenEvent
    data class OnDelete(val id: String) : LiveDataScreenEvent
    data class OnRename(val id: String) : LiveDataScreenEvent
    data class OnRenameNameChanged(val name: String) : LiveDataScreenEvent
    data object OnConfirmRename : LiveDataScreenEvent
    data object OnDismissRename : LiveDataScreenEvent
    data object OnConfirmDelete : LiveDataScreenEvent
    data object OnDismissDelete : LiveDataScreenEvent
    data object OnShareAll : LiveDataScreenEvent
    data object OnSaveAll : LiveDataScreenEvent
    data object OnToggleLogList : LiveDataScreenEvent
    data object OnReloadLogs : LiveDataScreenEvent
    data object OnFileFailure : LiveDataScreenEvent
    data object OnBack : LiveDataScreenEvent
    data object OnRead : LiveDataScreenEvent
    data object OnStart : LiveDataScreenEvent
    data object OnStop : LiveDataScreenEvent
}
