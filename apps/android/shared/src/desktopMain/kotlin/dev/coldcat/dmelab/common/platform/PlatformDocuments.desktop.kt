package dev.coldcat.dmelab.common.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import java.awt.EventQueue
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
internal actual fun rememberDocumentCreator(
    mimeType: String,
    persistPermission: Boolean,
    onFailure: () -> Unit,
    onResult: (String?) -> Unit,
): DocumentLauncher = rememberDesktopDocumentLauncher(
    save = true,
    mimeType = mimeType,
    onFailure = onFailure,
    onResult = onResult,
)

@Composable
internal actual fun rememberDocumentOpener(
    onFailure: () -> Unit,
    onResult: (String?) -> Unit,
): DocumentLauncher = rememberDesktopDocumentLauncher(
    save = false,
    mimeType = "application/octet-stream",
    onFailure = onFailure,
    onResult = onResult,
)

@Composable
private fun rememberDesktopDocumentLauncher(
    save: Boolean,
    mimeType: String,
    onFailure: () -> Unit,
    onResult: (String?) -> Unit,
): DocumentLauncher {
    val failure = rememberUpdatedState(onFailure)
    val result = rememberUpdatedState(onResult)
    return remember(save, mimeType) {
        object : DocumentLauncher {
            override fun launch(name: String) {
                val action = Runnable {
                    val dialog = try {
                        FileDialog(null as Frame?, "", if (save) FileDialog.SAVE else FileDialog.LOAD)
                    } catch (_: Exception) {
                        failure.value()
                        return@Runnable
                    }
                    val location = try {
                        dialog.isMultipleMode = false
                        if (save) dialog.file = name
                        if (!save) {
                            dialog.setFilenameFilter { _, filename -> filename.endsWith(".bin", ignoreCase = true) }
                        }
                        // The native modal dialog runs its own EDT event loop.
                        dialog.isVisible = true
                        dialog.file?.let { selected -> File(dialog.directory, selected).toURI().toString() }
                    } catch (_: Exception) {
                        failure.value()
                        return@Runnable
                    } finally {
                        dialog.dispose()
                    }
                    result.value(location)
                }
                if (EventQueue.isDispatchThread()) action.run() else EventQueue.invokeLater(action)
            }
        }
    }
}
