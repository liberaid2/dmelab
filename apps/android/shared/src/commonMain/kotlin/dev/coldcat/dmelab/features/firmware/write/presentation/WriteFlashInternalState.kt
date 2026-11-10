package dev.coldcat.dmelab.features.firmware.write.presentation

import dev.coldcat.dmelab.common.ecu.model.FirmwareMode
import dev.coldcat.dmelab.common.session.domain.EcuConnectionState
import dev.coldcat.dmelab.common.session.domain.EcuOperationState
import dev.coldcat.dmelab.common.session.domain.FirmwareImage
import dev.coldcat.dmelab.common.session.domain.OperationFailure

internal data class WriteFlashInternalState(
    val connection: EcuConnectionState,
    val operation: EcuOperationState,
    val mode: FirmwareMode = FirmwareMode.Full,
    val image: FirmwareImage? = null,
    val inspecting: Boolean = false,
    val confirmation: Boolean = false,
    val reviewedSessionId: Long? = null,
    val localFailure: OperationFailure? = null,
)
