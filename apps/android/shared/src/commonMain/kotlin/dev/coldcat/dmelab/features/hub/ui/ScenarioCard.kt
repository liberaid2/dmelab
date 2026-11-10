package dev.coldcat.dmelab.features.hub.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.common.ui.theme.DmeSizing
import dev.coldcat.dmelab.common.ui.theme.DmeSpacing
import dev.coldcat.dmelab.features.hub.presentation.HubScreenState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ScenarioCard(scenario: HubScreenState.Scenario, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(width = DmeSizing.border, color = MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = DmeSizing.touchTarget).padding(all = DmeSpacing.md),
            verticalArrangement = Arrangement.spacedBy(space = DmeSpacing.xs),
        ) {
            Text(text = stringResource(resource = scenario.title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text(text = stringResource(resource = scenario.description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


@Preview(name = "Плитка сценария", showBackground = true, widthDp = 176)
@Composable
private fun ScenarioCardPreview() {
    DMELabTheme {
        ScenarioCard(scenario = HubPreviewData.scenarios.first(), enabled = true, onClick = {})
    }
}
