package dev.coldcat.dmelab.features.diagnostics.livedata.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.common.platform.logSharingLabels
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.back_button
import dev.coldcat.dmelab.shared.resources.ic_arrow_back
import dev.coldcat.dmelab.shared.resources.live_log_clear_selection
import dev.coldcat.dmelab.shared.resources.live_log_selected_count
import dev.coldcat.dmelab.shared.resources.live_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LiveDataTopBar(onBack: () -> Unit, selectedCount: String? = null,
    canShareSelected: Boolean = false, onShareSelected: () -> Unit = {}) {
    TopAppBar(
        title = { Text(text = if (selectedCount == null) stringResource(Res.string.live_title)
            else stringResource(Res.string.live_log_selected_count, selectedCount), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(painter = painterResource(resource = Res.drawable.ic_arrow_back),
                    contentDescription = stringResource(if (selectedCount == null) Res.string.back_button else Res.string.live_log_clear_selection))
            }
        },
        actions = {
            if (selectedCount != null) TextButton(enabled = canShareSelected, onClick = onShareSelected) {
                Text(stringResource(logSharingLabels().action))
            }
        },
    )
}

@Preview(name = "Выбор логов", showBackground = true, widthDp = 360)
@Composable
private fun SelectionTopBarPreview() {
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = LocalDensity.current.density, fontScale = 1.3f,
        ),
    ) {
        DMELabTheme { LiveDataTopBar(onBack = {}, selectedCount = "3", canShareSelected = true) }
    }
}

@Preview(name = "Панель навигации", showBackground = true, widthDp = 360)
@Composable
private fun TopBarPreview() {
    DMELabTheme { LiveDataTopBar(onBack = {}) }
}
