package dev.coldcat.dmelab.features.firmware.write.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import dev.coldcat.dmelab.common.ui.theme.DmeSizing
import dev.coldcat.dmelab.common.ui.theme.DmeSpacing
import dev.coldcat.dmelab.common.ui.theme.DmeTypography
import dev.coldcat.dmelab.features.firmware.write.presentation.WriteFlashScreenEvent
import dev.coldcat.dmelab.features.firmware.write.presentation.WriteFlashScreenState
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.firmware_busy
import dev.coldcat.dmelab.shared.resources.firmware_disconnected
import dev.coldcat.dmelab.shared.resources.firmware_ignition
import dev.coldcat.dmelab.shared.resources.firmware_inspecting
import dev.coldcat.dmelab.shared.resources.firmware_preflight
import dev.coldcat.dmelab.shared.resources.firmware_total_time
import dev.coldcat.dmelab.shared.resources.firmware_write_calibration
import dev.coldcat.dmelab.shared.resources.firmware_write_full
import dev.coldcat.dmelab.shared.resources.firmware_write_success
import dev.coldcat.dmelab.shared.resources.mobile_change_file
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WriteFlashContent(state: WriteFlashScreenState, onEvent: (WriteFlashScreenEvent) -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { WriteFlashTopBar(onBack = { onEvent(WriteFlashScreenEvent.OnBack) }) },
        bottomBar = { WriteFlashActions(state = state, onEvent = onEvent) },
    ) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues = contentPadding).verticalScroll(state = rememberScrollState()).padding(all = DmeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(DmeSpacing.md),
        ) {
            WriteFlashIntro(showArtwork = state.result == null && !state.running)
            WriteFlashModeSelector(mode = state.mode, enabled = state.canStart, onMode = { onEvent(WriteFlashScreenEvent.OnMode(it)) })
            state.target?.let { Text(text = it, style = DmeTypography.identifier) }
            if (state.inspecting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(stringResource(Res.string.firmware_inspecting))
            }
            state.imageLabel?.let { Text(text = it, style = DmeTypography.identifier) }
            if (state.imageLabel != null) {
                OutlinedButton(
                    onClick = { onEvent(WriteFlashScreenEvent.OnChooseFile) },
                    enabled = state.canStart,
                    modifier = Modifier.fillMaxWidth().heightIn(min = DmeSizing.touchTarget),
                ) { Text(text = stringResource(resource = Res.string.mobile_change_file)) }
                Text(text = stringResource(resource = Res.string.firmware_preflight), style = MaterialTheme.typography.bodySmall)
            }
            if (state.running) WriteFlashProgress(progress = state.progress, label = state.progressLabel, elapsed = state.elapsed)
            else if (state.busy) Text(stringResource(Res.string.firmware_busy), color = DmeColors.warning.content)
            if (state.connectionRequired) {
                Text(stringResource(Res.string.firmware_disconnected), color = DmeColors.warning.content)
            }
            state.failure?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            state.result?.let {
                Text(stringResource(Res.string.firmware_write_success), style = MaterialTheme.typography.titleMedium, color = DmeColors.success.content)
                state.resultMode?.let { mode -> Text(stringResource(when (mode) {
                    FirmwareMode.Full -> Res.string.firmware_write_full
                    FirmwareMode.Calibration -> Res.string.firmware_write_calibration
                })) }
                state.resultDuration?.let { duration ->
                    Text(text = stringResource(resource = Res.string.firmware_total_time, formatArgs = arrayOf(duration)), style = MaterialTheme.typography.bodyMedium)
                }
                Text(text = it, style = MaterialTheme.typography.bodySmall)
                Text(stringResource(Res.string.firmware_ignition), color = DmeColors.warning.content)
            }
        }
    }
    if (state.confirmation) WriteFlashConfirmation(state = state, onEvent = onEvent)
}

@Preview(showBackground = true, locale = "ru", widthDp = 360, heightDp = 800)
@Composable
private fun ContentPreview() {
    DMELabTheme { WriteFlashContent(state = WriteFlashScreenState(canStart = true), onEvent = {}) }
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
