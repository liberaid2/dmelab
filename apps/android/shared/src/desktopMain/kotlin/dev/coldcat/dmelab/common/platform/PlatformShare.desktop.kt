package dev.coldcat.dmelab.common.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import java.io.File
import java.net.URI

@Composable
internal actual fun rememberFileSharer(onFailure: () -> Unit): FileSharer {
    val failure = rememberUpdatedState(onFailure)
    return remember {
        FileSharer { locations, _ ->
            try {
                require(locations.isNotEmpty())
                val files = locations.map { File(URI(it)) }
                check(files.all { it.isFile && it.canRead() })
                // Finder exposes the system Share menu for the selected files.
                ProcessBuilder(listOf("/usr/bin/open", "-R") + files.map { it.absolutePath }).start()
            } catch (_: Exception) {
                failure.value()
            }
        }
    }
}
