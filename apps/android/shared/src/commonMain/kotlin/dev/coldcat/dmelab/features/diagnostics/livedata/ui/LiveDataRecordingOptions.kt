package dev.coldcat.dmelab.features.diagnostics.livedata.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.common.ui.theme.DmeSpacing
import dev.coldcat.dmelab.features.diagnostics.livedata.presentation.LiveDataScreenEvent
import dev.coldcat.dmelab.features.diagnostics.livedata.presentation.LiveDataScreenState
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.live_log_record
import dev.coldcat.dmelab.shared.resources.live_log_record_note
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LiveDataRecordingOptions(state: LiveDataScreenState, onEvent: (LiveDataScreenEvent) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DmeSpacing.md)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DmeSpacing.xxs)) {
            Text(stringResource(Res.string.live_log_record), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(Res.string.live_log_record_note), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = state.record, enabled = state.activeOperation == null,
            onCheckedChange = { onEvent(LiveDataScreenEvent.OnRecordChanged(enabled = it)) })
    }
}

@Preview(name = "Запись CSV", showBackground = true, widthDp = 360)
@Composable
private fun LiveDataRecordingOptionsPreview() {
    DMELabTheme { LiveDataRecordingOptions(state = LiveDataScreenState(record = true), onEvent = {}) }
}
