package dev.coldcat.dmelab.features.firmware.write.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.coldcat.dmelab.common.ecu.model.FirmwareMode
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.common.ui.theme.DmeSpacing
import dev.coldcat.dmelab.features.firmware.write.presentation.WriteFlashScreenEvent
import dev.coldcat.dmelab.features.firmware.write.presentation.WriteFlashScreenState
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.firmware_confirm_details
import dev.coldcat.dmelab.shared.resources.firmware_confirm_title
import dev.coldcat.dmelab.shared.resources.firmware_confirm_write
import dev.coldcat.dmelab.shared.resources.firmware_not_now
import dev.coldcat.dmelab.shared.resources.firmware_write_calibration
import dev.coldcat.dmelab.shared.resources.firmware_write_full
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WriteFlashConfirmation(state: WriteFlashScreenState, onEvent: (WriteFlashScreenEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onEvent(WriteFlashScreenEvent.OnDismissConfirmation) },
        title = { Text(stringResource(Res.string.firmware_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(DmeSpacing.sm)) {
                state.target?.let { Text(text = it) }
                state.imageLabel?.let { Text(text = it) }
                Text(stringResource(when (state.mode) {
                    FirmwareMode.Full -> Res.string.firmware_write_full
                    FirmwareMode.Calibration -> Res.string.firmware_write_calibration
                }))
                Text(stringResource(Res.string.firmware_confirm_details))
            }
        },
        confirmButton = {
            TextButton(onClick = { onEvent(WriteFlashScreenEvent.OnConfirm) }) {
                Text(stringResource(Res.string.firmware_confirm_write))
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(WriteFlashScreenEvent.OnDismissConfirmation) }) {
                Text(stringResource(Res.string.firmware_not_now))
            }
        },
    )
}

@Preview(locale = "ru")
@Composable
private fun ConfirmationPreview() {
    DMELabTheme {
        WriteFlashConfirmation(
            state = WriteFlashScreenState(imageLabel = "calibration.bin · 65536 B", target = "Siemens MS43 · 430069"),
            onEvent = {},
        )
    }
}
