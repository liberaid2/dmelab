package dev.coldcat.dmelab.features.firmware.write.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.common.ui.theme.DmeSpacing
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.firmware_background
import dev.coldcat.dmelab.shared.resources.firmware_elapsed_time
import dev.coldcat.dmelab.shared.resources.firmware_keep_connected
import dev.coldcat.dmelab.shared.resources.firmware_write_running
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WriteFlashProgress(progress: Float?, label: String?, elapsed: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(DmeSpacing.sm)) {
        Text(stringResource(Res.string.firmware_write_running), style = MaterialTheme.typography.titleMedium)
        if (progress == null) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        else LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        elapsed?.let {
            Text(text = stringResource(resource = Res.string.firmware_elapsed_time, formatArgs = arrayOf(it)), style = MaterialTheme.typography.titleMedium)
        }
        label?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
        Text(stringResource(Res.string.firmware_keep_connected), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(Res.string.firmware_background), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview
@Composable
private fun ProgressPreview() {
    DMELabTheme { WriteFlashProgress(progress = 0.42f, label = null, elapsed = "01:24") }
}
