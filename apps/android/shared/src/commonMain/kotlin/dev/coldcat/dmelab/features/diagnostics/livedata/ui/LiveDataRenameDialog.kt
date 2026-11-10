package dev.coldcat.dmelab.features.diagnostics.livedata.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.common.ui.theme.DmeSpacing
import dev.coldcat.dmelab.features.diagnostics.livedata.presentation.LiveDataScreenEvent
import dev.coldcat.dmelab.features.diagnostics.livedata.presentation.LiveDataScreenState
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.action_cancel
import dev.coldcat.dmelab.shared.resources.live_log_invalid_name
import dev.coldcat.dmelab.shared.resources.live_log_name_hint
import dev.coldcat.dmelab.shared.resources.live_log_name_label
import dev.coldcat.dmelab.shared.resources.live_log_rename
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LiveDataRenameDialog(state: LiveDataScreenState.Rename, onEvent: (LiveDataScreenEvent) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = { onEvent(LiveDataScreenEvent.OnDismissRename) },
        title = { Text(stringResource(Res.string.live_log_rename)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(DmeSpacing.sm)) {
                OutlinedTextField(value = state.name,
                    onValueChange = { onEvent(LiveDataScreenEvent.OnRenameNameChanged(name = it)) },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    label = { Text(stringResource(Res.string.live_log_name_label)) },
                    suffix = { Text(state.suffix) }, singleLine = true, enabled = !state.busy,
                    isError = state.error != null,
                    supportingText = { Text(stringResource(state.error ?: Res.string.live_log_name_hint)) },
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (state.canConfirm) onEvent(LiveDataScreenEvent.OnConfirmRename)
                    }),
                )
                if (state.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(enabled = state.canConfirm, onClick = { onEvent(LiveDataScreenEvent.OnConfirmRename) }) {
            Text(stringResource(Res.string.live_log_rename))
        } },
        dismissButton = { TextButton(enabled = !state.busy, onClick = { onEvent(LiveDataScreenEvent.OnDismissRename) }) {
            Text(stringResource(Res.string.action_cancel))
        } },
    )
}

@Preview(name = "Переименование CSV", showBackground = true, widthDp = 360)
@Composable
private fun RenameDialogPreview() {
    DMELabTheme { LiveDataRenameDialog(state = LiveDataScreenState.Rename(name = "Поездка домой", suffix = ".csv", canConfirm = true), onEvent = {}) }
}

@Preview(name = "Имя частичного CSV · ошибка", showBackground = true, widthDp = 360)
@Composable
private fun RenameDialogErrorPreview() {
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = LocalDensity.current.density, fontScale = 1.3f,
        ),
    ) {
        DMELabTheme { LiveDataRenameDialog(state = LiveDataScreenState.Rename(name = "", suffix = "_partial.csv", canConfirm = false,
            error = Res.string.live_log_invalid_name), onEvent = {}) }
    }
}
