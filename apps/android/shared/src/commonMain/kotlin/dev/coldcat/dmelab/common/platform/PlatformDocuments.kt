package dev.coldcat.dmelab.common.platform

import androidx.compose.runtime.Composable

internal interface DocumentLauncher {
    fun launch(name: String = "")
}

@Composable
internal expect fun rememberDocumentCreator(
    mimeType: String,
    persistPermission: Boolean = false,
    onFailure: () -> Unit,
    onResult: (String?) -> Unit,
): DocumentLauncher

@Composable
internal expect fun rememberDocumentOpener(
    onFailure: () -> Unit,
    onResult: (String?) -> Unit,
): DocumentLauncher
