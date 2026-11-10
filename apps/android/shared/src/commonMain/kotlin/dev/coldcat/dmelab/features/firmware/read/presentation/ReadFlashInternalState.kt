package dev.coldcat.dmelab.features.firmware.read.presentation

import dev.coldcat.dmelab.common.ecu.model.FirmwareMode
import dev.coldcat.dmelab.common.session.domain.EcuConnectionState
import dev.coldcat.dmelab.common.session.domain.EcuOperationState
import dev.coldcat.dmelab.common.session.domain.OperationFailure

internal data class ReadFlashInternalState(
    val connection: EcuConnectionState,
    val operation: EcuOperationState,
    val mode: FirmwareMode = FirmwareMode.Full,
    val localFailure: OperationFailure? = null,
)
