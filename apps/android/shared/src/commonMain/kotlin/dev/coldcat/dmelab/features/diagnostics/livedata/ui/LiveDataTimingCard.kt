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
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.live_read_duration
import dev.coldcat.dmelab.shared.resources.live_update_frequency
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LiveDataTimingCard(readDuration: String, updateFrequency: String?, durationLabel: StringResource = Res.string.live_read_duration) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Column(
            modifier = Modifier.padding(all = DmeSpacing.md),
            verticalArrangement = Arrangement.spacedBy(space = DmeSpacing.xxs),
        ) {
            Text(
                text = stringResource(resource = durationLabel, formatArgs = arrayOf(readDuration)),
                style = MaterialTheme.typography.bodyMedium,
            )
            updateFrequency?.let { frequency ->
                Text(
                    text = stringResource(resource = Res.string.live_update_frequency, formatArgs = arrayOf(frequency)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(name = "Скорость опроса", showBackground = true, widthDp = 360)
@Composable
private fun TimingPreview() {
    DMELabTheme { LiveDataTimingCard(readDuration = "700", updateFrequency = "1,43") }
}
