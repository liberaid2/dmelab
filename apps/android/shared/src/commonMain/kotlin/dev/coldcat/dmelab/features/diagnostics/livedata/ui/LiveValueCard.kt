package dev.coldcat.dmelab.features.diagnostics.livedata.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.common.ui.theme.DmeSpacing
import dev.coldcat.dmelab.common.ui.theme.DmeTypography
import dev.coldcat.dmelab.features.diagnostics.livedata.presentation.LiveDataScreenState
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.live_command_active
import dev.coldcat.dmelab.shared.resources.live_command_inactive
import dev.coldcat.dmelab.shared.resources.live_unknown
import dev.coldcat.dmelab.shared.resources.live_value_unit
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LiveValueCard(value: LiveDataScreenState.Value) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.padding(DmeSpacing.md), verticalArrangement = Arrangement.spacedBy(DmeSpacing.xxs)) {
            Text(value.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val display = value.flag?.let {
                stringResource(if (it) Res.string.live_command_active else Res.string.live_command_inactive)
            } ?: stringResource(Res.string.live_value_unit, value.value ?: stringResource(Res.string.live_unknown), value.unit)
            Text(display, style = DmeTypography.identifier)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LiveValuePreview() {
    DMELabTheme { LiveValueCard(value = LiveDataScreenState.Value("Температура ОЖ", "92.3", "°C", null)) }
}
