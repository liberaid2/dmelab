package dev.coldcat.dmelab.features.firmware.write.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.common.ui.theme.DmeSizing
import dev.coldcat.dmelab.common.ui.theme.DmeSpacing
import dev.coldcat.dmelab.features.firmware.write.presentation.WriteFlashScreenEvent
import dev.coldcat.dmelab.features.firmware.write.presentation.WriteFlashScreenState
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.firmware_choose_file
import dev.coldcat.dmelab.shared.resources.firmware_inspecting
import dev.coldcat.dmelab.shared.resources.firmware_review_write
import dev.coldcat.dmelab.shared.resources.firmware_write_running
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WriteFlashActions(state: WriteFlashScreenState, onEvent: (WriteFlashScreenEvent) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = DmeSpacing.lg, vertical = DmeSpacing.md)) {
            Button(
                onClick = { onEvent(if (state.imageLabel == null) WriteFlashScreenEvent.OnChooseFile else WriteFlashScreenEvent.OnRequestConfirmation) },
                enabled = state.canStart,
                modifier = Modifier.fillMaxWidth().heightIn(min = DmeSizing.buttonHeight),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(text = stringResource(resource = when {
                        state.running -> Res.string.firmware_write_running
                        state.inspecting -> Res.string.firmware_inspecting
                        state.imageLabel == null -> Res.string.firmware_choose_file
                        else -> Res.string.firmware_review_write
                    }))
            }
        }
    }
}

@Preview(name = "Основное действие", showBackground = true, widthDp = 360)
@Composable
private fun ActionsPreview() {
    DMELabTheme { WriteFlashActions(state = WriteFlashScreenState(canStart = true), onEvent = {}) }
}
