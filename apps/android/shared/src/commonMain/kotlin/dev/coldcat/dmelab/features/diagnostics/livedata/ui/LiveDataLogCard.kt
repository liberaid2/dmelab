package dev.coldcat.dmelab.features.diagnostics.livedata.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.common.ui.theme.DmeSpacing
import dev.coldcat.dmelab.common.platform.logSharingLabels
import dev.coldcat.dmelab.features.diagnostics.livedata.presentation.LiveDataScreenEvent
import dev.coldcat.dmelab.features.diagnostics.livedata.presentation.LiveDataScreenState
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.live_log_delete
import dev.coldcat.dmelab.shared.resources.live_log_deselect
import dev.coldcat.dmelab.shared.resources.live_log_more
import dev.coldcat.dmelab.shared.resources.live_log_partial
import dev.coldcat.dmelab.shared.resources.live_log_partial_note
import dev.coldcat.dmelab.shared.resources.live_log_ready
import dev.coldcat.dmelab.shared.resources.live_log_recording
import dev.coldcat.dmelab.shared.resources.live_log_rename
import dev.coldcat.dmelab.shared.resources.live_log_save
import dev.coldcat.dmelab.shared.resources.live_log_select
import dev.coldcat.dmelab.shared.resources.live_log_stats
import dev.coldcat.dmelab.shared.resources.live_log_unavailable
import dev.coldcat.dmelab.shared.resources.live_log_wait
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LiveDataLogCard(log: LiveDataScreenState.Log, enabled: Boolean,
    onEvent: (LiveDataScreenEvent) -> Unit, selectionMode: Boolean = false) {
    var menuOpen by remember { mutableStateOf(false) }
    val selectable = !log.recording && log.available
    val selectLabel = stringResource(if (log.selected) Res.string.live_log_deselect else Res.string.live_log_select)
    Surface(color = if (log.selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.then(if (selectable) Modifier
            .semantics { selected = log.selected }
            .combinedClickable(enabled = enabled, role = if (selectionMode) Role.Checkbox else Role.Button,
                onLongClickLabel = selectLabel,
                onLongClick = { onEvent(LiveDataScreenEvent.OnToggleSelection(id = log.id)) },
                onClickLabel = if (selectionMode) selectLabel else stringResource(logSharingLabels().action),
                onClick = { onEvent(if (selectionMode) LiveDataScreenEvent.OnToggleSelection(id = log.id)
                    else LiveDataScreenEvent.OnShare(id = log.id)) }) else Modifier)
            .padding(DmeSpacing.md), verticalArrangement = Arrangement.spacedBy(DmeSpacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DmeSpacing.xs)) {
                Text(stringResource(if (!log.available) Res.string.live_log_unavailable else if (log.recording) Res.string.live_log_recording else if (log.partial) Res.string.live_log_partial else Res.string.live_log_ready),
                    modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                if (selectionMode && selectable) Checkbox(checked = log.selected, onCheckedChange = null, enabled = enabled)
            }
            Text(log.id, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(Res.string.live_log_stats, log.samples, log.duration, log.size), style = MaterialTheme.typography.bodySmall)
            if (log.recording || log.partial) Text(stringResource(if (log.recording) Res.string.live_log_wait else Res.string.live_log_partial_note),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (selectable && !selectionMode) Row(horizontalArrangement = Arrangement.spacedBy(DmeSpacing.xs)) {
                OutlinedButton(enabled = enabled, onClick = { onEvent(LiveDataScreenEvent.OnShare(id = log.id)) }) {
                    Text(stringResource(logSharingLabels().action))
                }
                Box {
                    TextButton(enabled = enabled, onClick = { menuOpen = true }) { Text(stringResource(Res.string.live_log_more)) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(enabled = enabled, text = { Text(stringResource(Res.string.live_log_select)) }, onClick = {
                            menuOpen = false
                            onEvent(LiveDataScreenEvent.OnToggleSelection(id = log.id))
                        })
                        DropdownMenuItem(enabled = enabled, text = { Text(stringResource(Res.string.live_log_rename)) }, onClick = {
                            menuOpen = false
                            onEvent(LiveDataScreenEvent.OnRename(id = log.id))
                        })
                        DropdownMenuItem(text = { Text(stringResource(Res.string.live_log_save)) }, onClick = {
                            menuOpen = false
                            onEvent(LiveDataScreenEvent.OnSave(id = log.id))
                        })
                        DropdownMenuItem(text = { Text(stringResource(Res.string.live_log_delete)) }, onClick = {
                            menuOpen = false
                            onEvent(LiveDataScreenEvent.OnDelete(id = log.id))
                        })
                    }
                }
            }
        }
    }
}

@Preview(name = "Выбранный CSV", showBackground = true, widthDp = 360)
@Composable
private fun SelectedLogPreview() {
    DMELabTheme {
        LiveDataLogCard(log = LiveDataScreenState.Log(id = "ms43_live_2026-11-05_12-30-00_example.csv",
            samples = "120", size = "48,2", duration = "02:00", selected = true),
            enabled = true, selectionMode = true, onEvent = {})
    }
}

@Preview(name = "Готовый CSV", showBackground = true, widthDp = 360)
@Composable
private fun LiveDataLogCardPreview() {
    DMELabTheme {
        LiveDataLogCard(log = LiveDataScreenState.Log(id = "ms43_live_20261103T123000000Z.csv", samples = "120", size = "48,2", duration = "02:00"),
            enabled = true, onEvent = {})
    }
}

@Preview(name = "Прерванный CSV · крупный шрифт", showBackground = true, widthDp = 360)
@Composable
private fun LiveDataPartialLogCardPreview() {
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = LocalDensity.current.density, fontScale = 1.3f,
        ),
    ) {
        DMELabTheme {
            LiveDataLogCard(log = LiveDataScreenState.Log(id = "ms43_live_20261103T123000000Z_partial.csv", samples = "18", size = "8,4", duration = "00:25", partial = true),
                enabled = true, onEvent = {})
        }
    }
}
