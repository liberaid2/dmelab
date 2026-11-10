package dev.coldcat.dmelab.features.firmware.read.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import cafe.adriel.voyager.core.screen.Screen
import dev.coldcat.dmelab.common.navigation.LocalRouter
import dev.coldcat.dmelab.common.platform.PlatformBackHandler
import dev.coldcat.dmelab.common.platform.rememberDocumentCreator
import dev.coldcat.dmelab.features.firmware.read.presentation.ReadFlashScreenCommand
import dev.coldcat.dmelab.features.firmware.read.presentation.ReadFlashScreenEvent
import dev.coldcat.dmelab.features.firmware.read.presentation.ReadFlashViewModel
import org.koin.compose.viewmodel.koinViewModel

internal class ReadFlashScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<ReadFlashViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val router = LocalRouter.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val picker = rememberDocumentCreator(
            mimeType = "application/octet-stream",
            persistPermission = true,
            onFailure = { viewModel.onEvent(ReadFlashScreenEvent.OnFileAccessFailure) },
            onResult = { location -> location?.let { viewModel.onEvent(ReadFlashScreenEvent.OnDestination(it)) } },
        )
        LaunchedEffect(viewModel, lifecycleOwner, router) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.commands.collect { command ->
                    when (command) {
                        ReadFlashScreenCommand.Back -> router.back()
                        is ReadFlashScreenCommand.ChooseDestination -> picker.launch(command.name)
                    }
                }
            }
        }
        PlatformBackHandler { viewModel.onEvent(ReadFlashScreenEvent.OnBack) }
        ReadFlashContent(state = state, onEvent = viewModel::onEvent)
    }
}
