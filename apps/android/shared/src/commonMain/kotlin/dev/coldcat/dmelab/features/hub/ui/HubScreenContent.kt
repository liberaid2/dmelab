package dev.coldcat.dmelab.features.hub.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.common.ui.theme.DmeSpacing
import dev.coldcat.dmelab.features.hub.presentation.HubScreenEvent
import dev.coldcat.dmelab.features.hub.presentation.HubScreenState
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.hub_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HubScreenContent(state: HubScreenState, onEvent: (HubScreenEvent) -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HubHeader(onChangeConnection = { onEvent(HubScreenEvent.OnChangeConnectionClicked) }, enabled = state.canChangeConnection) },
    ) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues = contentPadding)
                .verticalScroll(state = rememberScrollState()).padding(all = DmeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(space = DmeSpacing.lg),
        ) {
            Text(text = stringResource(resource = Res.string.hub_title), style = MaterialTheme.typography.headlineSmall)
            state.information?.let { information -> EcuSummary(information = information) }
            ScenarioGrid(
                scenarios = state.scenarios,
                enabled = state.information != null,
                onSelect = { scenario -> onEvent(HubScreenEvent.OnScenarioSelected(scenario = scenario)) },
            )
        }
    }
}

@Preview(name = "Сценарии MS43", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HubPreview() {
    DMELabTheme { HubScreenContent(state = HubPreviewData.state, onEvent = {}) }
}
