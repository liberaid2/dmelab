package dev.coldcat.dmelab.features.connection.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import dev.coldcat.dmelab.common.ui.theme.DmeColors
import dev.coldcat.dmelab.common.ui.theme.DmeSizing
import dev.coldcat.dmelab.common.ui.theme.DmeSpacing
import dev.coldcat.dmelab.features.connection.presentation.ConnectionScreenState
import dev.coldcat.dmelab.features.connection.presentation.ConnectionScreenState.Tone

@Composable
internal fun ConnectionNotice(notice: ConnectionScreenState.Notice, modifier: Modifier = Modifier) {
    val colors = when (notice.tone) {
        Tone.Neutral -> DmeColors.info
        Tone.Success -> DmeColors.success
        Tone.Attention -> DmeColors.warning
        Tone.Error -> DmeColors.error
    }
    Surface(
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        color = colors.container,
        border = BorderStroke(width = DmeSizing.border, color = colors.outline),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(modifier = Modifier.padding(all = DmeSpacing.md), horizontalArrangement = Arrangement.spacedBy(space = DmeSpacing.sm)) {
            Box(modifier = Modifier.padding(top = DmeSpacing.xs).size(DmeSizing.statusDot).background(color = colors.content, shape = CircleShape))
            Column(verticalArrangement = Arrangement.spacedBy(space = DmeSpacing.xs)) {
                Text(text = stringResource(resource = notice.title), style = MaterialTheme.typography.titleSmall, color = colors.content)
                Text(text = stringResource(resource = notice.description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
