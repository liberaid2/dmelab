package dev.coldcat.dmelab.features.firmware.open.presentation

import dev.coldcat.dmelab.common.session.domain.FirmwareImage
import dev.coldcat.dmelab.common.session.domain.OperationFailure

internal data class FirmwareOpenInternalState(
    val importing: Boolean = false,
    val image: FirmwareImage? = null,
    val failure: OperationFailure? = null,
)
