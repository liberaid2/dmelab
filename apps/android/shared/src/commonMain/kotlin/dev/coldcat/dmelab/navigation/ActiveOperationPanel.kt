package dev.coldcat.dmelab.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.coldcat.dmelab.common.navigation.Destination
import dev.coldcat.dmelab.common.session.domain.ActiveOperation
import dev.coldcat.dmelab.common.session.domain.OperationKind
import dev.coldcat.dmelab.common.session.domain.OperationProgress
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.common.ui.theme.DmeSpacing
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.firmware_elapsed_time
import dev.coldcat.dmelab.shared.resources.operation_clear_dtc
import dev.coldcat.dmelab.shared.resources.operation_live_data
import dev.coldcat.dmelab.shared.resources.operation_open
import dev.coldcat.dmelab.shared.resources.operation_read_dtc
import dev.coldcat.dmelab.shared.resources.operation_read_flash
import dev.coldcat.dmelab.shared.resources.operation_stop_live_data
import dev.coldcat.dmelab.shared.resources.operation_stopping
import dev.coldcat.dmelab.shared.resources.operation_write_flash
import org.jetbrains.compose.resources.stringResource

/** Application layout status, outside individual feature screens. */
@Composable
internal fun ActiveOperationPanel(operation: ActiveOperation, elapsed: String? = null, onOpen: () -> Unit, onStopLiveData: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = DmeSpacing.md, vertical = DmeSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(space = DmeSpacing.xxs),
        ) {
            val title = when (operation.kind) {
                OperationKind.ReadDtc -> Res.string.operation_read_dtc
                OperationKind.ClearDtc -> Res.string.operation_clear_dtc
                OperationKind.LiveData -> Res.string.operation_live_data
                OperationKind.ReadFlash -> Res.string.operation_read_flash
                OperationKind.WriteFlash -> Res.string.operation_write_flash
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(text = stringResource(resource = title), modifier = Modifier.weight(weight = 1f), style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = onOpen) { Text(text = stringResource(resource = Res.string.operation_open)) }
            }
            elapsed?.let {
                Text(text = stringResource(resource = Res.string.firmware_elapsed_time, formatArgs = arrayOf(it)), style = MaterialTheme.typography.bodySmall)
            }
            val progress = operation.progress
            if (progress != null && progress.total > 0L) {
                LinearProgressIndicator(
                    progress = { (progress.completed.toFloat() / progress.total.toFloat()).coerceIn(minimumValue = 0f, maximumValue = 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(space = DmeSpacing.xs)) {
                if (operation.kind == OperationKind.LiveData && operation.polling) {
                    TextButton(onClick = onStopLiveData, enabled = !operation.stopping) {
                        Text(text = stringResource(resource = if (operation.stopping) Res.string.operation_stopping else Res.string.operation_stop_live_data))
                    }
                }
            }
        }
    }
}

internal fun OperationKind.destination(): Destination = when (this) {
    OperationKind.ReadDtc, OperationKind.ClearDtc -> Destination.Dtc
    OperationKind.LiveData -> Destination.LiveData
    OperationKind.ReadFlash -> Destination.ReadFlash
    OperationKind.WriteFlash -> Destination.WriteFlash
}

@Preview(name = "Активная операция", showBackground = true)
@Composable
private fun ActiveOperationPanelPreview() {
    DMELabTheme {
        ActiveOperationPanel(
            operation = ActiveOperation(
                kind = OperationKind.ReadFlash,
                progress = OperationProgress(stage = "read", completed = 216, total = 512),
            ),
            elapsed = "01:24",
            onOpen = {},
            onStopLiveData = {},
        )
    }
}
