package dev.coldcat.dmelab.features.firmware.open.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.common.ui.theme.DmeSpacing
import dev.coldcat.dmelab.features.firmware.open.presentation.FirmwareOpenScreenState
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.firmware_inspecting
import dev.coldcat.dmelab.shared.resources.firmware_not_now
import dev.coldcat.dmelab.shared.resources.firmware_open_invalid
import dev.coldcat.dmelab.shared.resources.firmware_open_pending
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FirmwareOpenNotice(state: FirmwareOpenScreenState, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    if (!state.importing && state.image == null && state.message == null) return
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = modifier.fillMaxWidth().padding(all = DmeSpacing.md),
            verticalArrangement = Arrangement.spacedBy(DmeSpacing.xs)) {
            if (state.importing) {
                Text(stringResource(Res.string.firmware_inspecting))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            state.image?.let { Text(stringResource(Res.string.firmware_open_pending, it.name)) }
            state.message?.let { Text(stringResource(it)) }
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.firmware_not_now)) }
        }
    }
}

@Preview(name = "Входящий BIN · ошибка", widthDp = 360, showBackground = true)
@Composable
private fun FirmwareOpenNoticePreview() {
    DMELabTheme { FirmwareOpenNotice(state = FirmwareOpenScreenState(message = Res.string.firmware_open_invalid), onDismiss = {}) }
}
