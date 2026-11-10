package dev.coldcat.dmelab.features.diagnostics.livedata.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.common.ui.theme.DmeSpacing
import dev.coldcat.dmelab.common.platform.logSharingLabels
import dev.coldcat.dmelab.features.diagnostics.livedata.presentation.LiveDataScreenEvent
import dev.coldcat.dmelab.features.diagnostics.livedata.presentation.LiveDataScreenState
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.live_log_archive_count
import dev.coldcat.dmelab.shared.resources.live_log_archive_export
import dev.coldcat.dmelab.shared.resources.live_log_archive_note
import dev.coldcat.dmelab.shared.resources.live_log_archive_save
import dev.coldcat.dmelab.shared.resources.live_log_collapse
import dev.coldcat.dmelab.shared.resources.live_log_file_busy
import dev.coldcat.dmelab.shared.resources.live_log_show_all
import dev.coldcat.dmelab.shared.resources.live_log_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LiveDataLogsHeader(state: LiveDataScreenState, onEvent: (LiveDataScreenEvent) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(DmeSpacing.xs)) {
        Text(stringResource(Res.string.live_log_title), style = MaterialTheme.typography.titleMedium)
        state.logCount?.takeIf { state.selectedLogCount == null }?.let { count ->
            TextButton(onClick = { onEvent(LiveDataScreenEvent.OnToggleLogList) }) {
                Text(if (state.showAllLogs) stringResource(Res.string.live_log_collapse)
                    else stringResource(Res.string.live_log_show_all, count))
            }
        }
        if (state.selectedLogCount == null) Text(stringResource(Res.string.live_log_archive_count, state.exportableLogCount),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.selectedLogCount == null) Box {
            OutlinedButton(enabled = state.canExportAll, onClick = { menuOpen = true }) {
                Text(stringResource(Res.string.live_log_archive_export))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(enabled = state.canExportAll,
                    text = { Text(stringResource(logSharingLabels().archiveAction)) }, onClick = {
                        menuOpen = false
                        onEvent(LiveDataScreenEvent.OnShareAll)
                    })
                DropdownMenuItem(enabled = state.canExportAll,
                    text = { Text(stringResource(Res.string.live_log_archive_save)) }, onClick = {
                        menuOpen = false
                        onEvent(LiveDataScreenEvent.OnSaveAll)
                    })
            }
        }
        if (state.selectedLogCount == null) Text(stringResource(Res.string.live_log_archive_note), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(if (state.selectedLogCount == null) logSharingLabels().selectionHint else logSharingLabels().selectionActive),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.fileBusy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(stringResource(Res.string.live_log_file_busy), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(name = "Все логи · экспорт ZIP", showBackground = true, widthDp = 360)
@Composable
private fun LiveDataLogsHeaderPreview() {
    DMELabTheme {
        Surface {
            LiveDataLogsHeader(state = LiveDataScreenState(logCount = "8", exportableLogCount = "7", canExportAll = true), onEvent = {})
        }
    }
}

@Preview(name = "Все логи · крупный шрифт", showBackground = true, widthDp = 360)
@Composable
private fun LiveDataLogsHeaderLargeFontPreview() {
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = LocalDensity.current.density, fontScale = 1.3f,
        ),
    ) { LiveDataLogsHeaderPreview() }
}

@Preview(name = "Подготовка экспорта", showBackground = true, widthDp = 360)
@Composable
private fun LiveDataLogsHeaderBusyPreview() {
    DMELabTheme {
        Surface {
            LiveDataLogsHeader(state = LiveDataScreenState(logCount = "8", exportableLogCount = "8", fileBusy = true), onEvent = {})
        }
    }
}
