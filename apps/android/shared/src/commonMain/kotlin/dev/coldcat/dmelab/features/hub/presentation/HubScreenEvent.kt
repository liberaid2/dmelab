package dev.coldcat.dmelab.features.hub.presentation

internal sealed interface HubScreenEvent {
    data class OnScenarioSelected(val scenario: HubScenario) : HubScreenEvent
    data object OnChangeConnectionClicked : HubScreenEvent
}
