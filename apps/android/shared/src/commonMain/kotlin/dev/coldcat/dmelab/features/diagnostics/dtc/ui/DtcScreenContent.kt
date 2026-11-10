package dev.coldcat.dmelab.features.diagnostics.dtc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.common.ui.theme.DmeSpacing
import dev.coldcat.dmelab.features.diagnostics.dtc.presentation.DtcScreenEvent
import dev.coldcat.dmelab.features.diagnostics.dtc.presentation.DtcScreenState
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.diagnostics_busy_hint
import dev.coldcat.dmelab.shared.resources.diagnostics_connection_required
import dev.coldcat.dmelab.shared.resources.diagnostics_last_result
import dev.coldcat.dmelab.shared.resources.dtc_clear_acknowledged
import dev.coldcat.dmelab.shared.resources.dtc_empty
import dev.coldcat.dmelab.shared.resources.dtc_not_read
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DtcScreenContent(state: DtcScreenState, onEvent: (DtcScreenEvent) -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { DtcTopBar(onBack = { onEvent(DtcScreenEvent.OnBack) }) },
        bottomBar = { DtcActions(state = state, onEvent = onEvent) },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues = contentPadding),
            contentPadding = PaddingValues(DmeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(DmeSpacing.md),
        ) {
            item { DtcIntro(showArtwork = state.entries == null && state.activeOperation == null) }
            if (state.connectionRequired) item {
                Text(
                    text = stringResource(Res.string.diagnostics_connection_required),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.activeOperation?.let { operation ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(DmeSpacing.xs)) {
                        Text(stringResource(operation), style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(Res.string.diagnostics_busy_hint), style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            state.error?.let { error -> item {
                Text(stringResource(error), color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium)
            } }
            state.clearAcknowledgedAt?.let { time -> item {
                Text(stringResource(Res.string.dtc_clear_acknowledged, time),
                    style = MaterialTheme.typography.bodyMedium)
            } }
            state.updatedAt?.let { time -> item {
                Text(stringResource(Res.string.diagnostics_last_result, time),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
            when {
                state.entries == null -> item { Text(stringResource(Res.string.dtc_not_read)) }
                state.entries.isEmpty() -> item { Text(stringResource(Res.string.dtc_empty)) }
                else -> items(items = state.entries, key = { it.code }) { entry -> DtcEntryCard(entry = entry) }
            }
        }
    }
    if (state.confirmationVisible) {
        DtcClearDialog(
            onConfirm = { onEvent(DtcScreenEvent.OnConfirmClear) },
            onDismiss = { onEvent(DtcScreenEvent.OnDismissClear) },
            enabled = state.canClear,
        )
    }
}

@Preview(name = "Ошибки MS43", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun DtcScreenPreview() {
    DMELabTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            DtcScreenContent(
                state = DtcScreenState(
                    canRead = true,
                    canClear = true,
                    updatedAt = "03.11 14:25:00",
                    entries = listOf(DtcScreenState.Entry("18 · 0x12", "2", "00 00 00 00", "12 00 02 00 00 00 00 00 00 00")),
                ),
                onEvent = {},
            )
        }
    }
}
