package dev.coldcat.dmelab.features.diagnostics.dtc.ui

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
import dev.coldcat.dmelab.features.diagnostics.dtc.presentation.DtcScreenEvent
import dev.coldcat.dmelab.features.diagnostics.dtc.presentation.DtcScreenState
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.dtc_clear
import dev.coldcat.dmelab.shared.resources.dtc_read
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DtcActions(state: DtcScreenState, onEvent: (DtcScreenEvent) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = DmeSpacing.lg, vertical = DmeSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(space = DmeSpacing.sm),
        ) {
            Button(
                onClick = { onEvent(DtcScreenEvent.OnRead) },
                enabled = state.canRead,
                modifier = Modifier.weight(weight = 1f).heightIn(min = DmeSizing.buttonHeight),
                shape = MaterialTheme.shapes.medium,
            ) { Text(text = stringResource(resource = Res.string.dtc_read)) }
            OutlinedButton(
                onClick = { onEvent(DtcScreenEvent.OnClear) },
                enabled = state.canClear,
                modifier = Modifier.weight(weight = 1f).heightIn(min = DmeSizing.buttonHeight),
                shape = MaterialTheme.shapes.medium,
            ) { Text(text = stringResource(resource = Res.string.dtc_clear)) }
        }
    }
}

@Preview(name = "Действия сценария", showBackground = true, widthDp = 360)
@Composable
private fun ActionsPreview() {
    DMELabTheme { DtcActions(state = DtcScreenState(), onEvent = {}) }
}
