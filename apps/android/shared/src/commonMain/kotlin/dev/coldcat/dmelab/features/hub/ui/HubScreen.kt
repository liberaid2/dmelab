package dev.coldcat.dmelab.features.hub.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import cafe.adriel.voyager.core.screen.Screen
import dev.coldcat.dmelab.common.navigation.LocalRouter
import dev.coldcat.dmelab.features.hub.presentation.HubScreenCommand
import dev.coldcat.dmelab.features.hub.presentation.HubViewModel
import org.koin.compose.viewmodel.koinViewModel

internal class HubScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<HubViewModel>()
        val router = LocalRouter.current
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(viewModel, router, lifecycle) {
            lifecycle.repeatOnLifecycle(state = Lifecycle.State.STARTED) {
                viewModel.commands.collect { command ->
                    when (command) { is HubScreenCommand.Open -> router.open(destination = command.destination) }
                }
            }
        }
        val state by viewModel.state.collectAsStateWithLifecycle()
        HubScreenContent(state = state, onEvent = viewModel::onEvent)
    }
}
