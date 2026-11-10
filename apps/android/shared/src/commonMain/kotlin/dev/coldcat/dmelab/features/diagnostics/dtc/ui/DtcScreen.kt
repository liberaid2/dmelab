package dev.coldcat.dmelab.features.diagnostics.dtc.ui

import dev.coldcat.dmelab.common.platform.PlatformBackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import cafe.adriel.voyager.core.screen.Screen
import dev.coldcat.dmelab.common.navigation.LocalRouter
import dev.coldcat.dmelab.features.diagnostics.dtc.presentation.DtcScreenCommand
import dev.coldcat.dmelab.features.diagnostics.dtc.presentation.DtcScreenEvent
import dev.coldcat.dmelab.features.diagnostics.dtc.presentation.DtcViewModel
import org.koin.compose.viewmodel.koinViewModel

internal class DtcScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<DtcViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val router = LocalRouter.current
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(viewModel, router, lifecycle) {
            lifecycle.repeatOnLifecycle(state = Lifecycle.State.STARTED) {
                viewModel.commands.collect { command ->
                    when (command) {
                        DtcScreenCommand.Back -> router.back()
                    }
                }
            }
        }
        PlatformBackHandler { viewModel.onEvent(DtcScreenEvent.OnBack) }
        DtcScreenContent(state = state, onEvent = viewModel::onEvent)
    }
}
