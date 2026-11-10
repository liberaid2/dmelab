package dev.coldcat.dmelab.common.platform

import androidx.compose.runtime.Composable

internal fun interface FileSharer {
    fun share(locations: List<String>, mimeType: String)
}

@Composable
internal expect fun rememberFileSharer(onFailure: () -> Unit): FileSharer
