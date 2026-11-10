package dev.coldcat.dmelab.common.platform

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberDocumentCreator(
    mimeType: String,
    persistPermission: Boolean,
    onFailure: () -> Unit,
    onResult: (String?) -> Unit,
): DocumentLauncher {
    val context = LocalContext.current
    val failure = rememberUpdatedState(onFailure)
    val result = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(mimeType)) { uri ->
        try {
            if (uri != null && persistPermission) {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            result.value(uri?.toString())
        } catch (_: SecurityException) {
            failure.value()
        }
    }
    return remember(launcher) {
        object : DocumentLauncher {
            override fun launch(name: String) {
                try { launcher.launch(name) }
                catch (_: ActivityNotFoundException) { failure.value() }
                catch (_: SecurityException) { failure.value() }
            }
        }
    }
}

@Composable
internal actual fun rememberDocumentOpener(
    onFailure: () -> Unit,
    onResult: (String?) -> Unit,
): DocumentLauncher {
    val context = LocalContext.current
    val failure = rememberUpdatedState(onFailure)
    val result = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        try {
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            result.value(uri?.toString())
        } catch (_: SecurityException) {
            failure.value()
        }
    }
    return remember(launcher) {
        object : DocumentLauncher {
            override fun launch(name: String) {
                try { launcher.launch(arrayOf("application/octet-stream", "*/*")) }
                catch (_: ActivityNotFoundException) { failure.value() }
                catch (_: SecurityException) { failure.value() }
            }
        }
    }
}
