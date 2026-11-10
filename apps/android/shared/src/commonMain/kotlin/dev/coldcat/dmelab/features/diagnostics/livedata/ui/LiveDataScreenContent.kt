package dev.coldcat.dmelab.features.diagnostics.livedata.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.common.ui.theme.DmeSpacing
import dev.coldcat.dmelab.features.diagnostics.livedata.presentation.LiveDataScreenEvent
import dev.coldcat.dmelab.features.diagnostics.livedata.presentation.LiveDataScreenState
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.action_cancel
import dev.coldcat.dmelab.shared.resources.diagnostics_busy_hint
import dev.coldcat.dmelab.shared.resources.diagnostics_connection_required
import dev.coldcat.dmelab.shared.resources.diagnostics_last_result
import dev.coldcat.dmelab.shared.resources.diagnostics_polling
import dev.coldcat.dmelab.shared.resources.live_log_delete
import dev.coldcat.dmelab.shared.resources.live_log_delete_note
import dev.coldcat.dmelab.shared.resources.live_log_delete_title
import dev.coldcat.dmelab.shared.resources.live_log_file_error
import dev.coldcat.dmelab.shared.resources.live_log_reload
import dev.coldcat.dmelab.shared.resources.live_not_read
import dev.coldcat.dmelab.shared.resources.live_recovering
import dev.coldcat.dmelab.shared.resources.live_sample_interval
import dev.coldcat.dmelab.shared.resources.live_snapshot_note
import dev.coldcat.dmelab.shared.resources.live_stopped
import dev.coldcat.dmelab.shared.resources.live_stopping
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LiveDataScreenContent(state: LiveDataScreenState, onEvent: (LiveDataScreenEvent) -> Unit) {
    state.rename?.let { LiveDataRenameDialog(state = it, onEvent = onEvent) }
    state.deleteId?.let {
        AlertDialog(
            onDismissRequest = { onEvent(LiveDataScreenEvent.OnDismissDelete) },
            title = { Text(stringResource(Res.string.live_log_delete_title)) },
            text = { Text(stringResource(Res.string.live_log_delete_note)) },
            confirmButton = { TextButton(onClick = { onEvent(LiveDataScreenEvent.OnConfirmDelete) }) {
                Text(stringResource(Res.string.live_log_delete))
            } },
            dismissButton = { TextButton(onClick = { onEvent(LiveDataScreenEvent.OnDismissDelete) }) {
                Text(stringResource(Res.string.action_cancel))
            } },
        )
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { LiveDataTopBar(onBack = { onEvent(LiveDataScreenEvent.OnBack) },
            selectedCount = state.selectedLogCount, canShareSelected = state.canShareSelected,
            onShareSelected = { onEvent(LiveDataScreenEvent.OnShareSelected) }) },
        bottomBar = { LiveDataActions(state = state, onEvent = onEvent) },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues = contentPadding),
            contentPadding = PaddingValues(DmeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(DmeSpacing.md),
        ) {
            item { LiveDataIntro(showArtwork = state.values == null && state.activeOperation == null) }
            if (state.connectionRequired) item {
                Text(
                    text = stringResource(Res.string.diagnostics_connection_required),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.activeOperation?.let { operation -> item {
                Column(verticalArrangement = Arrangement.spacedBy(DmeSpacing.xs)) {
                    Text(stringResource(if (state.stopping) Res.string.live_stopping else operation))
                    if (!state.polling) Text(stringResource(Res.string.diagnostics_busy_hint), style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            } }
            state.error?.let { error -> item {
                Text(stringResource(error), color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium)
            } }
            state.recoveryCount?.let { count -> item {
                Text(stringResource(Res.string.live_recovering, count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
            item { LiveDataGroupOptions(state = state, onEvent = onEvent) }
            item { LiveDataRecordingOptions(state = state, onEvent = onEvent) }
            state.fileMessage?.let { message -> item {
                Column {
                    Text(stringResource(message), style = MaterialTheme.typography.bodyMedium)
                    if (message == Res.string.live_log_file_error) TextButton(
                        enabled = !state.fileBusy,
                        onClick = { onEvent(LiveDataScreenEvent.OnReloadLogs) },
                    ) { Text(stringResource(Res.string.live_log_reload)) }
                }
            } }
            if (state.logs.any { !it.available }) item {
                TextButton(enabled = !state.fileBusy,
                    onClick = { onEvent(LiveDataScreenEvent.OnReloadLogs) }) {
                    Text(stringResource(Res.string.live_log_reload))
                }
            }
            if (state.logs.isNotEmpty()) {
                item {
                    LiveDataLogsHeader(state = state, onEvent = onEvent)
                }
                items(items = state.logs, key = { it.id }) { log ->
                    LiveDataLogCard(log = log, enabled = !state.fileBusy, onEvent = onEvent,
                        selectionMode = state.selectedLogCount != null)
                }
            }
            state.readDuration?.let { duration -> item {
                LiveDataTimingCard(readDuration = duration, updateFrequency = state.updateFrequency)
            } }
            state.sampleInterval?.let { interval -> item {
                LiveDataTimingCard(readDuration = interval, updateFrequency = state.updateFrequency,
                    durationLabel = Res.string.live_sample_interval)
            } }
            state.updatedAt?.let { time -> item {
                Column(verticalArrangement = Arrangement.spacedBy(DmeSpacing.xxs)) {
                    Text(stringResource(Res.string.diagnostics_last_result, time), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!state.polling) Text(stringResource(Res.string.live_stopped), style = MaterialTheme.typography.bodySmall)
                }
            } }
            if (state.values == null) item { Text(stringResource(Res.string.live_not_read)) }
            state.values?.let { values -> items(items = values, key = { it.label }) { value -> LiveValueCard(value = value) } }
            item { Text(stringResource(Res.string.live_snapshot_note), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Preview(name = "Параметры MS43", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun LiveDataPreview() {
    DMELabTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LiveDataScreenContent(
                state = LiveDataScreenState(
                    canStart = true,
                    updatedAt = "03.11 14:25:00",
                    readDuration = "700",
                    updateFrequency = "1,43",
                    values = listOf(LiveDataScreenState.Value("Обороты двигателя", "752", "об/мин", null)),
                ),
                onEvent = {},
            )
        }
    }
}

@Preview(name = "Восстановление опроса", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun LiveDataRecoveringPreview() {
    DMELabTheme {
        LiveDataScreenContent(state = LiveDataScreenState(polling = true, canStop = true,
            activeOperation = Res.string.diagnostics_polling, recoveryCount = "3",
            updatedAt = "03.11 14:25:00", sampleInterval = "700", updateFrequency = "1,43",
            values = listOf(LiveDataScreenState.Value("Обороты двигателя", "752", "об/мин", null))), onEvent = {})
    }
}
