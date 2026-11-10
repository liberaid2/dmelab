package dev.coldcat.dmelab.features.hub.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.common.ui.theme.DmeSizing
import dev.coldcat.dmelab.common.ui.theme.DmeSpacing
import dev.coldcat.dmelab.common.ui.theme.DmeTypography
import dev.coldcat.dmelab.features.hub.presentation.HubScreenState
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.ecu_card_title
import dev.coldcat.dmelab.shared.resources.hub_flash_label
import dev.coldcat.dmelab.shared.resources.hub_hardware_label
import dev.coldcat.dmelab.shared.resources.hub_last_identification
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EcuSummary(information: HubScreenState.Information) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(width = DmeSizing.border, color = MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(all = DmeSpacing.md),
            verticalArrangement = Arrangement.spacedBy(space = DmeSpacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = DmeSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(resource = Res.string.ecu_card_title), modifier = Modifier.weight(weight = 1f), style = MaterialTheme.typography.titleMedium)
                Text(text = information.firmwareVersion, style = DmeTypography.identifier, color = MaterialTheme.colorScheme.primary)
            }
            Identifier(label = Res.string.hub_hardware_label, value = information.hardwareId)
            Identifier(label = Res.string.hub_flash_label, value = information.flashIdentifier)
            Text(
                text = stringResource(resource = Res.string.hub_last_identification),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Identifier(label: StringResource, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(space = DmeSpacing.sm), verticalAlignment = Alignment.Top) {
        Text(text = stringResource(resource = label), modifier = Modifier.weight(weight = 1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, modifier = Modifier.weight(weight = 2f), style = MaterialTheme.typography.bodyMedium)
    }
}


@Preview(name = "Карточка ЭБУ", showBackground = true, widthDp = 360)
@Composable
private fun EcuSummaryPreview() {
    DMELabTheme { EcuSummary(information = HubPreviewData.information) }
}
