package dev.coldcat.dmelab.features.firmware.write.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.coldcat.dmelab.common.ecu.model.FirmwareMode
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.common.ui.theme.DmeSizing
import dev.coldcat.dmelab.common.ui.theme.DmeSpacing
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.firmware_write_calibration
import dev.coldcat.dmelab.shared.resources.firmware_write_full
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WriteFlashModeSelector(mode: FirmwareMode, enabled: Boolean, onMode: (FirmwareMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(DmeSpacing.xs)) {
        FirmwareMode.entries.forEach { option ->
            FilterChip(
                selected = mode == option,
                enabled = enabled,
                onClick = { onMode(option) },
                label = { Text(stringResource(when (option) {
                    FirmwareMode.Full -> Res.string.firmware_write_full
                    FirmwareMode.Calibration -> Res.string.firmware_write_calibration
                })) },
                modifier = Modifier.fillMaxWidth().heightIn(min = DmeSizing.touchTarget),
            )
        }
    }
}

@Preview
@Composable
private fun ModePreview() {
    DMELabTheme { WriteFlashModeSelector(mode = FirmwareMode.Full, enabled = true, onMode = {}) }
}
