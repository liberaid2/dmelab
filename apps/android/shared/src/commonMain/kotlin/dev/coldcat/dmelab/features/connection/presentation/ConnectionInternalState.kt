package dev.coldcat.dmelab.features.connection.presentation

import dev.coldcat.dmelab.common.session.domain.EcuConnectionState

internal data class ConnectionInternalState(
    val connection: EcuConnectionState,
    val step: ConnectionScreenState.Step,
)
