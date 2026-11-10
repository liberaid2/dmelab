package dev.coldcat.dmelab.features.diagnostics.dtc.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.diagnostics_cancel
import dev.coldcat.dmelab.shared.resources.dtc_clear_confirm
import dev.coldcat.dmelab.shared.resources.dtc_clear_title
import dev.coldcat.dmelab.shared.resources.dtc_clear_warning
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DtcClearDialog(onConfirm: () -> Unit, onDismiss: () -> Unit, enabled: Boolean) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.dtc_clear_title)) },
        text = { Text(stringResource(Res.string.dtc_clear_warning)) },
        confirmButton = { TextButton(onClick = onConfirm, enabled = enabled) {
            Text(stringResource(Res.string.dtc_clear_confirm))
        } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.diagnostics_cancel)) } },
    )
}

@Preview(showBackground = true)
@Composable
private fun DtcClearPreview() {
    DMELabTheme { DtcClearDialog(onConfirm = {}, onDismiss = {}, enabled = true) }
}
