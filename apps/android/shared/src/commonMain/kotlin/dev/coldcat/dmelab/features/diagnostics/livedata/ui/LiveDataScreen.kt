package dev.coldcat.dmelab.features.diagnostics.livedata.ui

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
import dev.coldcat.dmelab.common.platform.rememberFileSharer
import dev.coldcat.dmelab.features.diagnostics.livedata.presentation.LiveDataExportTarget
import dev.coldcat.dmelab.features.diagnostics.livedata.presentation.LiveDataScreenCommand
import dev.coldcat.dmelab.features.diagnostics.livedata.presentation.LiveDataScreenEvent
import dev.coldcat.dmelab.features.diagnostics.livedata.presentation.LiveDataViewModel
import org.koin.compose.viewmodel.koinViewModel

internal class LiveDataScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<LiveDataViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val router = LocalRouter.current
        val csvPicker = rememberDocumentCreator(
            mimeType = "text/csv",
            onFailure = { viewModel.onEvent(LiveDataScreenEvent.OnFileFailure) },
            onResult = { viewModel.onEvent(LiveDataScreenEvent.OnDestination(uri = it)) },
        )
        val archivePicker = rememberDocumentCreator(
            mimeType = "application/zip",
            onFailure = { viewModel.onEvent(LiveDataScreenEvent.OnFileFailure) },
            onResult = { viewModel.onEvent(LiveDataScreenEvent.OnDestination(uri = it)) },
        )
        val sharer = rememberFileSharer(onFailure = { viewModel.onEvent(LiveDataScreenEvent.OnFileFailure) })
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(viewModel, router, lifecycle) {
            lifecycle.repeatOnLifecycle(state = Lifecycle.State.STARTED) {
                viewModel.commands.collect { command ->
                    when (command) {
                        LiveDataScreenCommand.Back -> router.back()
                        is LiveDataScreenCommand.ChooseDestination -> {
                            when (command.format) {
                                LiveDataExportTarget.Format.Csv -> csvPicker.launch(command.name)
                                LiveDataExportTarget.Format.Zip -> archivePicker.launch(command.name)
                            }
                        }
                        is LiveDataScreenCommand.Share -> {
                            sharer.share(locations = command.uris, mimeType = command.format.mimeType)
                        }
                    }
                }
            }
        }
        PlatformBackHandler { viewModel.onEvent(LiveDataScreenEvent.OnBack) }
        LiveDataScreenContent(state = state, onEvent = viewModel::onEvent)
    }
}
