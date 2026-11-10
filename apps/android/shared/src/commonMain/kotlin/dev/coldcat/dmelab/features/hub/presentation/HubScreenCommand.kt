package dev.coldcat.dmelab.features.hub.presentation

import dev.coldcat.dmelab.common.navigation.Destination

internal sealed interface HubScreenCommand {
    data class Open(val destination: Destination) : HubScreenCommand
}
