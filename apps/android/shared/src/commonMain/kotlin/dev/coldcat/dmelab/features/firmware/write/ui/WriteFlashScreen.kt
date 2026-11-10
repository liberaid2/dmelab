package dev.coldcat.dmelab.features.firmware.write.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import cafe.adriel.voyager.core.screen.Screen
import dev.coldcat.dmelab.common.ecu.model.FirmwareMode
import dev.coldcat.dmelab.common.navigation.LocalRouter
import dev.coldcat.dmelab.common.platform.PlatformBackHandler
import dev.coldcat.dmelab.common.platform.rememberDocumentOpener
import dev.coldcat.dmelab.features.firmware.write.presentation.WriteFlashScreenCommand
import dev.coldcat.dmelab.features.firmware.write.presentation.WriteFlashScreenEvent
import dev.coldcat.dmelab.features.firmware.write.presentation.WriteFlashViewModel
import org.koin.compose.viewmodel.koinViewModel

internal class WriteFlashScreen(
    private val initialFile: String? = null,
    private val initialMode: FirmwareMode = FirmwareMode.Full,
) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<WriteFlashViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val router = LocalRouter.current
        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(viewModel, initialFile) {
            initialFile?.let { viewModel.onEvent(WriteFlashScreenEvent.OnExternalFile(location = it, mode = initialMode)) }
        }
        val picker = rememberDocumentOpener(
            onFailure = { viewModel.onEvent(WriteFlashScreenEvent.OnFileAccessFailure) },
            onResult = { location -> location?.let { viewModel.onEvent(WriteFlashScreenEvent.OnFile(it)) } },
        )
        LaunchedEffect(viewModel, lifecycleOwner, router) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.commands.collect { command ->
                    when (command) {
                        WriteFlashScreenCommand.Back -> router.back()
                        WriteFlashScreenCommand.ChooseFile -> picker.launch()
                    }
                }
            }
        }
        PlatformBackHandler { viewModel.onEvent(WriteFlashScreenEvent.OnBack) }
        WriteFlashContent(state = state, onEvent = viewModel::onEvent)
    }
}
