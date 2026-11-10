package dev.coldcat.dmelab.features.diagnostics.dtc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.common.ui.theme.DmeSpacing
import dev.coldcat.dmelab.common.ui.theme.DmeTypography
import dev.coldcat.dmelab.features.diagnostics.dtc.presentation.DtcScreenState
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.dtc_data
import dev.coldcat.dmelab.shared.resources.dtc_frequency
import dev.coldcat.dmelab.shared.resources.dtc_raw
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DtcEntryCard(entry: DtcScreenState.Entry) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.padding(DmeSpacing.md), verticalArrangement = Arrangement.spacedBy(DmeSpacing.xs)) {
            Text(entry.code, style = DmeTypography.identifier)
            Text(stringResource(Res.string.dtc_frequency, entry.frequency), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(Res.string.dtc_data, entry.data), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(Res.string.dtc_raw, entry.raw), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DtcEntryPreview() {
    DMELabTheme { DtcEntryCard(entry = DtcScreenState.Entry("18 · 0x12", "2", "00 00 00 00", "12 00 02 00 00 00 00 00 00 00")) }
}
