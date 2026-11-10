package dev.coldcat.dmelab.features.diagnostics.dtc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
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
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.dtc_description
import dev.coldcat.dmelab.shared.resources.ic_diagnostics
import dev.coldcat.dmelab.shared.resources.mobile_dtc_heading
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DtcIntro(showArtwork: Boolean) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(space = DmeSpacing.md)) {
        if (showArtwork) {
            Surface(
                modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.large,
            ) {
                Box(modifier = Modifier.size(size = DmeSizing.featureArtwork), contentAlignment = Alignment.Center) {
                    Icon(painter = painterResource(resource = Res.drawable.ic_diagnostics), contentDescription = null, modifier = Modifier.size(size = DmeSpacing.xl), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text(text = stringResource(resource = Res.string.mobile_dtc_heading), style = MaterialTheme.typography.headlineMedium)
        }
        Text(text = stringResource(resource = Res.string.dtc_description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(name = "Подготовка сценария", showBackground = true, widthDp = 360)
@Composable
private fun IntroPreview() {
    DMELabTheme { DtcIntro(showArtwork = true) }
}
