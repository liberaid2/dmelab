package dev.coldcat.dmelab.features.hub.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.common.ui.theme.DmeSpacing
import dev.coldcat.dmelab.features.hub.presentation.HubScenario
import dev.coldcat.dmelab.features.hub.presentation.HubScreenState

@Composable
internal fun ScenarioGrid(
    scenarios: List<HubScreenState.Scenario>,
    enabled: Boolean,
    onSelect: (HubScenario) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(space = DmeSpacing.sm)) {
        scenarios.chunked(size = 2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().height(intrinsicSize = IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(space = DmeSpacing.sm),
            ) {
                row.forEach { scenario ->
                    ScenarioCard(
                        scenario = scenario,
                        enabled = enabled,
                        modifier = Modifier.weight(weight = 1f).fillMaxHeight(),
                        onClick = { onSelect(scenario.id) },
                    )
                }
            }
        }
    }
}

@Preview(name = "Сетка сценариев", showBackground = true, widthDp = 360)
@Composable
private fun ScenarioGridPreview() {
    DMELabTheme { ScenarioGrid(scenarios = HubPreviewData.scenarios, enabled = true, onSelect = {}) }
}
