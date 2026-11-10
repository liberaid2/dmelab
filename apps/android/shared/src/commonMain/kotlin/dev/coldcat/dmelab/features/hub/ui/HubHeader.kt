package dev.coldcat.dmelab.features.hub.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.app_name
import dev.coldcat.dmelab.shared.resources.hub_change_connection
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HubHeader(onChangeConnection: () -> Unit, enabled: Boolean = true) {
    TopAppBar(
        title = { Text(text = stringResource(resource = Res.string.app_name)) },
        actions = {
            TextButton(onClick = onChangeConnection, enabled = enabled) {
                Text(text = stringResource(resource = Res.string.hub_change_connection))
            }
        },
    )
}

@Preview(name = "Главная · App bar", showBackground = true, widthDp = 360)
@Composable
private fun HubHeaderPreview() {
    DMELabTheme { HubHeader(onChangeConnection = {}) }
}
