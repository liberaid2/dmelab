package dev.coldcat.dmelab.features.firmware.read.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import dev.coldcat.dmelab.common.ecu.model.FirmwareMode
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.common.ui.theme.DmeColors
import dev.coldcat.dmelab.common.ui.theme.DmeSpacing
import dev.coldcat.dmelab.features.firmware.read.presentation.ReadFlashScreenEvent
import dev.coldcat.dmelab.features.firmware.read.presentation.ReadFlashScreenState
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.firmware_busy
import dev.coldcat.dmelab.shared.resources.firmware_disconnected
import dev.coldcat.dmelab.shared.resources.firmware_read_calibration
import dev.coldcat.dmelab.shared.resources.firmware_read_full
import dev.coldcat.dmelab.shared.resources.firmware_read_success
import dev.coldcat.dmelab.shared.resources.firmware_total_time
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ReadFlashContent(state: ReadFlashScreenState, onEvent: (ReadFlashScreenEvent) -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ReadFlashTopBar(onBack = { onEvent(ReadFlashScreenEvent.OnBack) }) },
        bottomBar = { ReadFlashActions(state = state, onEvent = onEvent) },
    ) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues = contentPadding).verticalScroll(state = rememberScrollState()).padding(all = DmeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(DmeSpacing.md),
        ) {
            ReadFlashIntro(showArtwork = state.result == null && !state.running)
            ReadFlashModeSelector(mode = state.mode, enabled = state.canStart, onMode = { onEvent(ReadFlashScreenEvent.OnMode(it)) })
            if (state.running) ReadFlashProgress(progress = state.progress, label = state.progressLabel, elapsed = state.elapsed)
            else if (state.busy) Text(stringResource(Res.string.firmware_busy), color = DmeColors.warning.content)
            if (state.connectionRequired) {
                Text(stringResource(Res.string.firmware_disconnected), color = DmeColors.warning.content)
            }
            state.failure?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            state.result?.let {
                Text(stringResource(Res.string.firmware_read_success), style = MaterialTheme.typography.titleMedium, color = DmeColors.success.content)
                state.resultMode?.let { mode -> Text(stringResource(when (mode) {
                    FirmwareMode.Full -> Res.string.firmware_read_full
                    FirmwareMode.Calibration -> Res.string.firmware_read_calibration
                })) }
                state.resultDuration?.let { duration ->
                    Text(text = stringResource(resource = Res.string.firmware_total_time, formatArgs = arrayOf(duration)), style = MaterialTheme.typography.bodyMedium)
                }
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Preview(showBackground = true, locale = "ru", widthDp = 360, heightDp = 800)
@Composable
private fun ContentPreview() {
    DMELabTheme { ReadFlashContent(state = ReadFlashScreenState(canStart = true), onEvent = {}) }
}

@Preview(name = "Небольшой экран · крупный шрифт", showBackground = true, locale = "ru", widthDp = 360, heightDp = 640)
@Composable
private fun LargeFontContentPreview() {
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = LocalDensity.current.density, fontScale = 1.3f,
        ),
    ) { ContentPreview() }
}
