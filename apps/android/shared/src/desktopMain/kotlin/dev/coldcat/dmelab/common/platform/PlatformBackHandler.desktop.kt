package dev.coldcat.dmelab.common.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Composable
internal actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val currentEnabled = rememberUpdatedState(enabled)
    val currentOnBack = rememberUpdatedState(onBack)
    val key = remember { Any() }
    DisposableEffect(key) {
        DesktopBackDispatcher.register(key = key) {
            if (currentEnabled.value) {
                currentOnBack.value()
                true
            } else false
        }
        onDispose { DesktopBackDispatcher.unregister(key = key) }
    }
}
