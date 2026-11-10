package dev.coldcat.dmelab.features.hub.presentation

import dev.coldcat.dmelab.common.session.domain.EcuConnectionState

internal data class HubInternalState(val connection: EcuConnectionState, val busy: Boolean = false)
