package dev.coldcat.dmelab.features.firmware.write.presentation

import dev.coldcat.dmelab.common.ecu.model.FirmwareMode
import org.jetbrains.compose.resources.StringResource

internal data class WriteFlashScreenState(
    val mode: FirmwareMode = FirmwareMode.Full,
    val canStart: Boolean = false,
    val connectionRequired: Boolean = false,
    val busy: Boolean = false,
    val running: Boolean = false,
    val progress: Float? = null,
    val progressLabel: String? = null,
    val elapsed: String? = null,
    val resultDuration: String? = null,
    val failure: StringResource? = null,
    val result: String? = null,
    val resultMode: FirmwareMode? = null,
    val imageLabel: String? = null,
    val target: String? = null,
    val inspecting: Boolean = false,
    val confirmation: Boolean = false,
)
