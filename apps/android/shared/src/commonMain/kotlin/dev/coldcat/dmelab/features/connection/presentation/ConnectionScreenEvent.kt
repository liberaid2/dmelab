package dev.coldcat.dmelab.features.connection.presentation

internal sealed interface ConnectionScreenEvent {
    data object OnLogsClicked : ConnectionScreenEvent
    data object OnContinueClicked : ConnectionScreenEvent
    data object OnIdentifyClicked : ConnectionScreenEvent
    data object OnPermissionClicked : ConnectionScreenEvent
    data object OnRefreshClicked : ConnectionScreenEvent
    data object OnBackClicked : ConnectionScreenEvent
    data object OnResumed : ConnectionScreenEvent
    data class OnAdapterSelected(val id: String) : ConnectionScreenEvent
}
