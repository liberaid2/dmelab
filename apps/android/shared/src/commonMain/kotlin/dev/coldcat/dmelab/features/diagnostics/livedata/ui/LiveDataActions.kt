package dev.coldcat.dmelab.features.diagnostics.livedata.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.common.ui.theme.DmeSizing
import dev.coldcat.dmelab.common.ui.theme.DmeSpacing
import dev.coldcat.dmelab.features.diagnostics.livedata.presentation.LiveDataScreenEvent
import dev.coldcat.dmelab.features.diagnostics.livedata.presentation.LiveDataScreenState
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.live_once
import dev.coldcat.dmelab.shared.resources.live_start
import dev.coldcat.dmelab.shared.resources.live_stop
import dev.coldcat.dmelab.shared.resources.mobile_stopping
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LiveDataActions(state: LiveDataScreenState, onEvent: (LiveDataScreenEvent) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = DmeSpacing.lg, vertical = DmeSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(space = DmeSpacing.sm),
        ) {
            Button(
                onClick = { onEvent(if (state.polling) LiveDataScreenEvent.OnStop else LiveDataScreenEvent.OnStart) },
                enabled = state.canStart || state.canStop,
                modifier = Modifier.weight(weight = 1f).heightIn(min = DmeSizing.buttonHeight),
                shape = MaterialTheme.shapes.medium,
            ) { Text(text = stringResource(resource = if (state.stopping) Res.string.mobile_stopping else if (state.polling) Res.string.live_stop else Res.string.live_start)) }
            OutlinedButton(
                onClick = { onEvent(LiveDataScreenEvent.OnRead) },
                enabled = state.canStart,
                modifier = Modifier.weight(weight = 1f).heightIn(min = DmeSizing.buttonHeight),
                shape = MaterialTheme.shapes.medium,
            ) { Text(text = stringResource(resource = Res.string.live_once)) }
        }
    }
}

@Preview(name = "Действия сценария", showBackground = true, widthDp = 360)
@Composable
private fun ActionsPreview() {
    DMELabTheme { LiveDataActions(state = LiveDataScreenState(), onEvent = {}) }
}
