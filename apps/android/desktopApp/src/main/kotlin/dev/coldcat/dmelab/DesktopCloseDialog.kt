package dev.coldcat.dmelab

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.coldcat.dmelab.common.session.domain.OperationKind
import dev.coldcat.dmelab.common.session.domain.OperationRuntime
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.desktop_close_body
import dev.coldcat.dmelab.shared.resources.desktop_close_stay
import dev.coldcat.dmelab.shared.resources.desktop_close_stop
import dev.coldcat.dmelab.shared.resources.desktop_close_title
import dev.coldcat.dmelab.shared.resources.desktop_close_wait
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DesktopCloseDialog(runtime: OperationRuntime, onDismiss: () -> Unit, onClose: () -> Unit) {
    val state by runtime.state.collectAsStateWithLifecycle()
    val polling = state.active?.let { it.kind == OperationKind.LiveData && it.polling } == true
    var waiting by remember { mutableStateOf(false) }
    LaunchedEffect(state.active, waiting) {
        if (state.active == null) {
            if (waiting) onClose() else onDismiss()
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.desktop_close_title)) },
        text = { Text(stringResource(Res.string.desktop_close_body)) },
        confirmButton = {
            TextButton(enabled = !waiting, onClick = {
                waiting = true
                if (polling) runtime.stopLiveData()
            }) { Text(stringResource(if (polling) Res.string.desktop_close_stop else Res.string.desktop_close_wait)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.desktop_close_stay)) } },
    )
}
