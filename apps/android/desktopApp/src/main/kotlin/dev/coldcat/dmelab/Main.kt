package dev.coldcat.dmelab

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import dev.coldcat.dmelab.common.platform.DesktopBackDispatcher
import dev.coldcat.dmelab.common.session.data.DesktopOperationHost
import dev.coldcat.dmelab.common.session.domain.OperationRuntime
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.features.firmware.open.presentation.FirmwareOpenViewModel
import java.awt.Desktop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.cancel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.qualifier.named
import org.koin.plugin.module.dsl.startKoin

fun main(args: Array<String>) {
    val graph = startKoin<DesktopApplication>()
    val owner = DesktopViewModelOwner()
    val runtime = graph.koin.get<OperationRuntime>()
    val scope = graph.koin.get<CoroutineScope>(qualifier = named("application"))
    val executor = graph.koin.get<kotlinx.coroutines.CoroutineDispatcher>(qualifier = named("ecu"))
    graph.koin.get<DesktopOperationHost>().attach(runtime = runtime)
    try {
        application {
            var closeRequested by remember { mutableStateOf(false) }
            fun requestClose() {
                if (runtime.state.value.active == null) exitApplication() else closeRequested = true
            }
            Window(
                onCloseRequest = ::requestClose,
                title = "DME Lab",
                state = rememberWindowState(width = 560.dp, height = 900.dp),
                onPreviewKeyEvent = { event ->
                    event.type == KeyEventType.KeyDown && event.key == Key.Escape && DesktopBackDispatcher.onBack()
                },
            ) {
                DisposableEffect(Unit) {
                    val desktop = Desktop.getDesktop()
                    val supportsQuit = desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)
                    if (supportsQuit) desktop.setQuitHandler { _, response ->
                        response.cancelQuit()
                        requestClose()
                    }
                    onDispose { if (supportsQuit) desktop.setQuitHandler(null) }
                }
                CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                    DMELabTheme {
                        val firmwareOpen = koinViewModel<FirmwareOpenViewModel>()
                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            args.firstOrNull()?.let { source -> firmwareOpen.open(location = java.io.File(source).toURI().toString(), newRequest = true) }
                        }
                        DmeLabApp(firmwareOpen = firmwareOpen, notificationPermission = { })
                        if (closeRequested) DesktopCloseDialog(runtime = runtime,
                            onDismiss = { closeRequested = false }, onClose = ::exitApplication)
                    }
                }
            }
        }
    } finally {
        owner.viewModelStore.clear()
        scope.cancel()
        (executor as? ExecutorCoroutineDispatcher)?.close()
        graph.close()
    }
}
