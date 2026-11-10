package dev.coldcat.dmelab.features.hub.presentation

import org.jetbrains.compose.resources.StringResource

internal data class HubScreenState(
    val information: Information?,
    val scenarios: List<Scenario>,
    val canChangeConnection: Boolean = true,
) {
    data class Information(val hardwareId: String, val flashIdentifier: String, val firmwareVersion: String)
    data class Scenario(val id: HubScenario, val title: StringResource, val description: StringResource)
}
