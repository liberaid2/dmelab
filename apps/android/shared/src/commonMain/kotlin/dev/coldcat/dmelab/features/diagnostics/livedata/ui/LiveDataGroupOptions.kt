package dev.coldcat.dmelab.features.diagnostics.livedata.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import dev.coldcat.dmelab.common.ecu.model.LiveDataGroup
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.common.ui.theme.DmeSpacing
import dev.coldcat.dmelab.features.diagnostics.livedata.presentation.LiveDataScreenEvent
import dev.coldcat.dmelab.features.diagnostics.livedata.presentation.LiveDataScreenState
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.live_group_digital
import dev.coldcat.dmelab.shared.resources.live_group_digital_description
import dev.coldcat.dmelab.shared.resources.live_group_measurements
import dev.coldcat.dmelab.shared.resources.live_group_measurements_description
import dev.coldcat.dmelab.shared.resources.live_group_vanos
import dev.coldcat.dmelab.shared.resources.live_group_vanos_description
import dev.coldcat.dmelab.shared.resources.live_groups_empty
import dev.coldcat.dmelab.shared.resources.live_groups_locked
import dev.coldcat.dmelab.shared.resources.live_groups_note
import dev.coldcat.dmelab.shared.resources.live_groups_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LiveDataGroupOptions(state: LiveDataScreenState, onEvent: (LiveDataScreenEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(DmeSpacing.xs)) {
        Text(stringResource(Res.string.live_groups_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(Res.string.live_groups_note), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.groups.forEach { group ->
            Surface(shape = MaterialTheme.shapes.medium) {
                Row(modifier = Modifier.fillMaxWidth().toggleable(value = group.selected,
                    enabled = state.canChangeGroups, role = Role.Checkbox,
                    onValueChange = { onEvent(LiveDataScreenEvent.OnGroupChanged(group = group.id, enabled = it)) })
                    .padding(all = DmeSpacing.sm), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DmeSpacing.sm)) {
                    Checkbox(checked = group.selected, onCheckedChange = null, enabled = state.canChangeGroups)
                    Column(verticalArrangement = Arrangement.spacedBy(DmeSpacing.xxs)) {
                        Text(stringResource(group.title), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(group.description), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (state.emptyGroupSelection) Text(stringResource(Res.string.live_groups_empty),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        if (!state.canChangeGroups) Text(stringResource(Res.string.live_groups_locked),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(name = "Набор Live Data", showBackground = true, widthDp = 360)
@Composable
private fun LiveDataGroupOptionsPreview() {
    DMELabTheme {
        LiveDataGroupOptions(state = LiveDataScreenState(groups = listOf(
            LiveDataScreenState.Group(LiveDataGroup.Measurements, Res.string.live_group_measurements,
                Res.string.live_group_measurements_description, true),
            LiveDataScreenState.Group(LiveDataGroup.Vanos, Res.string.live_group_vanos,
                Res.string.live_group_vanos_description, false),
            LiveDataScreenState.Group(LiveDataGroup.Digital, Res.string.live_group_digital,
                Res.string.live_group_digital_description, false),
        )), onEvent = {})
    }
}

@Preview(name = "Набор Live Data · крупный шрифт", showBackground = true, widthDp = 360)
@Composable
private fun LiveDataGroupOptionsLargeFontPreview() {
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = LocalDensity.current.density, fontScale = 1.3f,
        ),
    ) { LiveDataGroupOptionsPreview() }
}
