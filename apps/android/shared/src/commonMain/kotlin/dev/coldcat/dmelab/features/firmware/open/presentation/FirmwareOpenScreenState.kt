package dev.coldcat.dmelab.features.firmware.open.presentation

import dev.coldcat.dmelab.common.session.domain.FirmwareImage
import org.jetbrains.compose.resources.StringResource

internal data class FirmwareOpenScreenState(
    val importing: Boolean = false,
    val image: FirmwareImage? = null,
    val message: StringResource? = null,
)
