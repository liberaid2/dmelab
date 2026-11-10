package dev.coldcat.dmelab.features.diagnostics.dtc.presentation

import dev.coldcat.dmelab.common.session.domain.EcuConnectionState
import dev.coldcat.dmelab.common.session.domain.EcuOperationState

internal data class DtcInternalState(
    val connection: EcuConnectionState,
    val operations: EcuOperationState,
    val confirmingSessionId: Long? = null,
)
