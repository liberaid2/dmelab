package dev.coldcat.dmelab.features.firmware.read.ui

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
import dev.coldcat.dmelab.features.firmware.read.presentation.ReadFlashScreenEvent
import dev.coldcat.dmelab.features.firmware.read.presentation.ReadFlashScreenState
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.firmware_choose_destination
import dev.coldcat.dmelab.shared.resources.firmware_read_running
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ReadFlashActions(state: ReadFlashScreenState, onEvent: (ReadFlashScreenEvent) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = DmeSpacing.lg, vertical = DmeSpacing.md)) {
            Button(
                onClick = { onEvent(ReadFlashScreenEvent.OnChooseDestination) },
                enabled = state.canStart,
                modifier = Modifier.fillMaxWidth().heightIn(min = DmeSizing.buttonHeight),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(text = stringResource(resource = if (state.running) Res.string.firmware_read_running else Res.string.firmware_choose_destination))
            }
        }
    }
}

@Preview(name = "Основное действие", showBackground = true, widthDp = 360)
@Composable
private fun ActionsPreview() {
    DMELabTheme { ReadFlashActions(state = ReadFlashScreenState(canStart = true), onEvent = {}) }
}
