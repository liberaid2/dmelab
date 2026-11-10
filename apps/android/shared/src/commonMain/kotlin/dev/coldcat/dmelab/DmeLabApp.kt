package dev.coldcat.dmelab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreProvider
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import dev.coldcat.dmelab.common.navigation.Destination
import dev.coldcat.dmelab.common.navigation.LocalRouter
import dev.coldcat.dmelab.common.navigation.Router
import dev.coldcat.dmelab.common.session.domain.EcuConnection
import dev.coldcat.dmelab.common.session.domain.EcuConnectionState.Operation
import dev.coldcat.dmelab.common.session.domain.EcuOperations
import dev.coldcat.dmelab.common.session.domain.OperationFailure
import dev.coldcat.dmelab.common.session.domain.OperationKind
import dev.coldcat.dmelab.common.session.presentation.formatOperationDuration
import dev.coldcat.dmelab.common.ui.theme.DmeColors
import dev.coldcat.dmelab.common.ui.theme.DmeSpacing
import dev.coldcat.dmelab.features.connection.ui.ConnectionScreen
import dev.coldcat.dmelab.features.firmware.open.presentation.FirmwareOpenScreenEvent
import dev.coldcat.dmelab.features.firmware.open.presentation.FirmwareOpenViewModel
import dev.coldcat.dmelab.features.firmware.open.ui.FirmwareOpenNotice
import dev.coldcat.dmelab.features.hub.ui.HubScreen
import dev.coldcat.dmelab.navigation.ActiveOperationPanel
import dev.coldcat.dmelab.navigation.VoyagerRouter
import dev.coldcat.dmelab.navigation.destination
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.operation_interrupted
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun DmeLabApp(firmwareOpen: FirmwareOpenViewModel, notificationPermission: @Composable (Boolean) -> Unit) {
    val connection = koinInject<EcuConnection>()
    val operations = koinInject<EcuOperations>()
    val state by connection.state.collectAsStateWithLifecycle()
    val operationState by operations.state.collectAsStateWithLifecycle()
    val incoming by firmwareOpen.state.collectAsStateWithLifecycle()
    val ready = state.operation is Operation.Identified
    notificationPermission(operationState.active != null)
    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) { connection.refresh() }
    val provider = rememberViewModelStoreProvider(key = "voyager-entries")
    Navigator(screen = if (ready) HubScreen() else ConnectionScreen()) { navigator ->
        val router: Router = remember(navigator) { VoyagerRouter(navigator = navigator) }
        val current = navigator.lastItem.destination()
        val showActivePanel = operationState.active?.kind?.destination()?.let { it != current } == true
        val showInterrupted = operationState.failure == OperationFailure.Interrupted
        val showIncoming = incoming.importing || incoming.image != null || incoming.message != null
        val rootOwnsBottomInsets = showActivePanel || showInterrupted || showIncoming
        // Do not discard an operation screen or its cached results after USB loss.
        LaunchedEffect(ready, current) {
            if (ready && current == Destination.Connection) router.setRoot(destination = Destination.Hub)
            if (!ready && current == Destination.Hub) {
                router.setRoot(destination = Destination.Connection)
            }
        }
        LaunchedEffect(ready, current, incoming.image, operationState.active) {
            incoming.image?.let { image ->
                if (operationState.active == null) {
                    if (ready && current != Destination.Connection) {
                        router.openFirmware(source = image.location, mode = image.mode)
                        firmwareOpen.onEvent(FirmwareOpenScreenEvent.OnDelivered(location = image.location))
                    } else if (!ready && current != Destination.Connection) {
                        router.setRoot(destination = Destination.Connection)
                    }
                }
            }
        }
        val entryKeys = navigator.items.map { it.key }
        var previousKeys by rememberSaveable { mutableStateOf(emptyList<String>()) }
        LaunchedEffect(entryKeys) {
            (previousKeys - entryKeys.toSet()).forEach { provider.clearKey(key = it) }
            previousKeys = entryKeys
        }
        CompositionLocalProvider(LocalRouter provides router) {
            Column(modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.background)) {
                Box(modifier = Modifier.weight(weight = 1f).then(
                    if (rootOwnsBottomInsets) Modifier.consumeWindowInsets(insets = WindowInsets.navigationBars) else Modifier,
                )) {
                    key(navigator.lastItem.key) {
                        val owner = rememberViewModelStoreOwner(key = navigator.lastItem.key, provider = provider)
                        CompositionLocalProvider(LocalViewModelStoreOwner provides owner) { CurrentScreen() }
                    }
                }
                FirmwareOpenNotice(state = incoming,
                    modifier = if (showActivePanel || showInterrupted) Modifier else Modifier.navigationBarsPadding(),
                    onDismiss = { firmwareOpen.onEvent(FirmwareOpenScreenEvent.OnDismiss) })
                if (showInterrupted) {
                    Surface(color = DmeColors.warning.container, contentColor = DmeColors.warning.content) {
                        Text(
                            text = stringResource(resource = Res.string.operation_interrupted),
                            modifier = (if (showActivePanel) Modifier else Modifier.navigationBarsPadding()).padding(all = DmeSpacing.md),
                        )
                    }
                }
                operationState.active?.takeIf { showActivePanel }?.let { active ->
                    ActiveOperationPanel(
                        operation = active,
                        elapsed = active.takeIf { it.kind == OperationKind.ReadFlash || it.kind == OperationKind.WriteFlash }
                            ?.let { formatOperationDuration(elapsedMillis = it.elapsedMillis) },
                        onOpen = { router.open(destination = active.kind.destination()) },
                        onStopLiveData = operations::stopLiveData,
                    )
                }
            }
        }
    }
}
