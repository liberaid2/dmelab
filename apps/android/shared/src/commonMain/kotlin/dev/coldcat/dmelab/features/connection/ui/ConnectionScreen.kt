package dev.coldcat.dmelab.features.connection.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import cafe.adriel.voyager.core.screen.Screen
import dev.coldcat.dmelab.common.ecu.model.EcuInformation
import dev.coldcat.dmelab.common.ecu.model.UsbAdapter
import dev.coldcat.dmelab.common.ecu.model.UsbInventory
import dev.coldcat.dmelab.common.navigation.Destination
import dev.coldcat.dmelab.common.navigation.LocalRouter
import dev.coldcat.dmelab.common.platform.PlatformBackHandler
import dev.coldcat.dmelab.common.session.domain.EcuConnectionState
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.common.ui.theme.DmeColors
import dev.coldcat.dmelab.common.ui.theme.DmeSizing
import dev.coldcat.dmelab.common.ui.theme.DmeSpacing
import dev.coldcat.dmelab.common.ui.theme.DmeTypography
import dev.coldcat.dmelab.features.connection.presentation.ConnectionInternalState
import dev.coldcat.dmelab.features.connection.presentation.ConnectionScreenCommand
import dev.coldcat.dmelab.features.connection.presentation.ConnectionScreenEvent
import dev.coldcat.dmelab.features.connection.presentation.ConnectionScreenMapper
import dev.coldcat.dmelab.features.connection.presentation.ConnectionScreenState
import dev.coldcat.dmelab.features.connection.presentation.ConnectionScreenState.Step
import dev.coldcat.dmelab.features.connection.presentation.ConnectionViewModel
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.adapter_section
import dev.coldcat.dmelab.shared.resources.cable_obd_body
import dev.coldcat.dmelab.shared.resources.cable_obd_title
import dev.coldcat.dmelab.features.connection.presentation.connectionInstructions
import dev.coldcat.dmelab.shared.resources.ecu_card_title
import dev.coldcat.dmelab.shared.resources.ecu_connection_body
import dev.coldcat.dmelab.shared.resources.ecu_connection_title
import dev.coldcat.dmelab.shared.resources.ecu_firmware_label
import dev.coldcat.dmelab.shared.resources.ecu_flash_label
import dev.coldcat.dmelab.shared.resources.ecu_footer
import dev.coldcat.dmelab.shared.resources.ecu_hardware_label
import dev.coldcat.dmelab.shared.resources.ecu_ignition_body
import dev.coldcat.dmelab.shared.resources.ecu_ignition_title
import dev.coldcat.dmelab.shared.resources.usb_refresh
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** USB setup and explicit MS43 identification within one state-driven onboarding flow. */
internal class ConnectionScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<ConnectionViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val router = LocalRouter.current
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(viewModel, router, lifecycle) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.commands.collect { command ->
                    when (command) {
                        ConnectionScreenCommand.OpenLogs -> router.open(destination = Destination.LiveData)
                    }
                }
            }
        }
        LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
            viewModel.onEvent(event = ConnectionScreenEvent.OnResumed)
        }
        PlatformBackHandler(enabled = state.step == Step.Ecu || state.isBusy) {
            viewModel.onEvent(event = ConnectionScreenEvent.OnBackClicked)
        }
        ConnectionScreenContent(state = state, onEvent = viewModel::onEvent)
    }
}

@Composable
private fun ConnectionScreenContent(state: ConnectionScreenState, onEvent: (ConnectionScreenEvent) -> Unit) {
    val instructions = connectionInstructions()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ConnectionTopBar(step = state.step, onBack = { onEvent(ConnectionScreenEvent.OnBackClicked) },
            onLogs = { onEvent(ConnectionScreenEvent.OnLogsClicked) }) },
        bottomBar = { ConnectionActions(state = state, onEvent = onEvent) },
    ) { contentPadding ->
        key(state.step) {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues = contentPadding)
                    .verticalScroll(state = rememberScrollState()).padding(horizontal = DmeSpacing.lg, vertical = DmeSpacing.md),
                verticalArrangement = Arrangement.spacedBy(space = DmeSpacing.lg),
            ) {
                if (state.information == null || state.step == Step.Cable) ConnectionIllustration(step = state.step)
                Column(verticalArrangement = Arrangement.spacedBy(space = DmeSpacing.xs)) {
                    Text(text = stringResource(resource = state.title), style = MaterialTheme.typography.headlineMedium)
                    Text(text = stringResource(resource = state.description), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                when (state.step) {
                    Step.Cable -> {
                        Column(verticalArrangement = Arrangement.spacedBy(space = DmeSpacing.md)) {
                            Instruction(number = 1, title = instructions.cableTitle, body = instructions.cableBody)
                            Instruction(number = 2, title = Res.string.cable_obd_title, body = Res.string.cable_obd_body)
                        }
                        if (state.adapters.isNotEmpty()) AdapterChoices(state = state, onEvent = onEvent)
                    }
                    Step.Ecu -> {
                        if (state.information == null) {
                            Column(verticalArrangement = Arrangement.spacedBy(space = DmeSpacing.md)) {
                                Instruction(number = 1, title = Res.string.ecu_ignition_title, body = Res.string.ecu_ignition_body)
                                Instruction(number = 2, title = Res.string.ecu_connection_title, body = Res.string.ecu_connection_body)
                            }
                        } else {
                            EcuInformationCard(information = state.information)
                        }
                    }
                }
                ConnectionNotice(notice = state.notice)
                if (state.step == Step.Cable && !state.isBusy) {
                    TextButton(onClick = { onEvent(ConnectionScreenEvent.OnRefreshClicked) }, modifier = Modifier.align(alignment = Alignment.CenterHorizontally).heightIn(min = DmeSizing.touchTarget)) {
                        Text(text = stringResource(resource = Res.string.usb_refresh))
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionActions(state: ConnectionScreenState, onEvent: (ConnectionScreenEvent) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = DmeSpacing.lg, vertical = DmeSpacing.md),
            verticalArrangement = Arrangement.spacedBy(space = DmeSpacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(space = DmeSpacing.sm)) {
                Button(
                    onClick = { state.primary.event?.let(onEvent) },
                    enabled = state.primary.event != null,
                    modifier = Modifier.weight(weight = 1f).heightIn(min = DmeSizing.buttonHeight),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = DmeColors.surfaceHigh,
                        disabledContentColor = DmeColors.textMuted,
                    ),
                ) {
                    if (state.primary.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(DmeSizing.icon), strokeWidth = DmeSizing.progressStroke)
                        Spacer(modifier = Modifier.width(DmeSpacing.xs))
                    }
                    Text(text = stringResource(resource = state.primary.label), style = MaterialTheme.typography.labelLarge)
                }
            }
            Text(
                text = stringResource(resource = when (state.step) { Step.Cable -> connectionInstructions().footer; Step.Ecu -> Res.string.ecu_footer }),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Instruction(number: Int, title: StringResource, body: StringResource) {
    Row(horizontalArrangement = Arrangement.spacedBy(space = DmeSpacing.md)) {
        Box(modifier = Modifier.size(DmeSizing.marker).background(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = CircleShape), contentAlignment = Alignment.Center) {
            Text(text = number.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(verticalArrangement = Arrangement.spacedBy(space = DmeSpacing.xxs)) {
            Text(text = stringResource(resource = title), style = MaterialTheme.typography.titleSmall)
            Text(text = stringResource(resource = body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AdapterChoices(state: ConnectionScreenState, onEvent: (ConnectionScreenEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(space = DmeSpacing.xs)) {
        Text(text = stringResource(resource = Res.string.adapter_section), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.adapters.forEach { adapter ->
            val selected = adapter.id == state.selectedId
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(width = DmeSizing.border, color = if (selected) DmeColors.accentOutline else MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().selectable(
                        selected = selected,
                        enabled = !state.isBusy,
                        role = Role.RadioButton,
                        onClick = { onEvent(ConnectionScreenEvent.OnAdapterSelected(id = adapter.id)) },
                    ).padding(all = DmeSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space = DmeSpacing.sm),
                ) {
                    if (state.adapters.size > 1) RadioButton(selected = selected, onClick = null, enabled = !state.isBusy)
                    Column {
                        Text(text = adapter.name, style = MaterialTheme.typography.titleSmall)
                        Text(text = adapter.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun EcuInformationCard(information: ConnectionScreenState.EcuCard) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large, border = BorderStroke(width = DmeSizing.border, color = MaterialTheme.colorScheme.outlineVariant)) {
        Column(modifier = Modifier.fillMaxWidth().padding(all = DmeSpacing.lg), verticalArrangement = Arrangement.spacedBy(space = DmeSpacing.lg)) {
            Text(text = stringResource(resource = Res.string.ecu_card_title), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            InformationRow(label = Res.string.ecu_hardware_label, value = information.hardwareId)
            InformationRow(label = Res.string.ecu_flash_label, value = information.flashIdentifier)
            InformationRow(label = Res.string.ecu_firmware_label, value = information.firmwareVersion)
        }
    }
}

@Composable
private fun InformationRow(label: StringResource, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(space = DmeSpacing.xxs)) {
        Text(text = stringResource(resource = label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = DmeTypography.identifier)
    }
}

@Preview(name = "Кабель", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun CablePreview() {
    val internal = ConnectionInternalState(
        connection = EcuConnectionState(
            inventory = UsbInventory.Available(adapters = emptyList(), unsupportedCount = 0, permissionPendingId = null, permissionDeniedId = null),
            selectedId = null,
            operation = EcuConnectionState.Operation.Idle,
        ),
        step = Step.Cable,
    )
    DMELabTheme {
        ConnectionScreenContent(state = ConnectionScreenMapper().getScreenState(internal = internal), onEvent = {})
    }
}

@Preview(name = "Проверка ЭБУ", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun IdentificationPreview() {
    DMELabTheme {
        ConnectionScreenContent(state = identificationPreviewState(identified = false), onEvent = {})
    }
}

@Preview(name = "Кабель · небольшой экран", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun CableLargeFontPreview() {
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = LocalDensity.current.density, fontScale = 1.3f,
        ),
    ) { CablePreview() }
}

@Preview(name = "ЭБУ определён", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun IdentifiedPreview() {
    DMELabTheme {
        ConnectionScreenContent(state = identificationPreviewState(identified = true), onEvent = {})
    }
}

private fun identificationPreviewState(identified: Boolean): ConnectionScreenState {
    val adapter = UsbAdapter(id = "preview", name = "FTDI K-line", vendorId = 0x0403, productId = 0x6001, hasPermission = true)
    val operation = if (identified) {
        EcuConnectionState.Operation.Identified(
            information = EcuInformation(hardwareId = "MS43 preview", flashIdentifier = "AM29F400BB", firmwareVersion = "430069"),
        )
    } else {
        EcuConnectionState.Operation.Idle
    }
    return ConnectionScreenMapper().getScreenState(
        internal = ConnectionInternalState(
            connection = EcuConnectionState(
                inventory = UsbInventory.Available(adapters = listOf(adapter), unsupportedCount = 0, permissionPendingId = null, permissionDeniedId = null),
                selectedId = adapter.id,
                operation = operation,
            ),
            step = Step.Ecu,
        ),
    )
}
