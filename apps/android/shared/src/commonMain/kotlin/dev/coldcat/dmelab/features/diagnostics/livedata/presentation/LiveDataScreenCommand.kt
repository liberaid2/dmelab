package dev.coldcat.dmelab.features.diagnostics.livedata.presentation
internal sealed interface LiveDataScreenCommand {
    data class Share(val uris: List<String>, val format: LiveDataExportTarget.Format) : LiveDataScreenCommand
    data class ChooseDestination(val name: String, val format: LiveDataExportTarget.Format) : LiveDataScreenCommand
    data object Back : LiveDataScreenCommand
}
