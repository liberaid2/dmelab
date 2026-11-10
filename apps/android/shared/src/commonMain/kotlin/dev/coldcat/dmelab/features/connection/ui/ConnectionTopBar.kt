package dev.coldcat.dmelab.features.connection.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.features.connection.presentation.ConnectionScreenState.Step
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.back_button
import dev.coldcat.dmelab.shared.resources.ic_arrow_back
import dev.coldcat.dmelab.shared.resources.live_log_open
import dev.coldcat.dmelab.shared.resources.mobile_connection_title
import dev.coldcat.dmelab.shared.resources.mobile_step_cable
import dev.coldcat.dmelab.shared.resources.mobile_step_ecu
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConnectionTopBar(step: Step, onBack: () -> Unit, onLogs: () -> Unit) {
    TopAppBar(
        title = {
            Column {
                Text(text = stringResource(resource = Res.string.mobile_connection_title), style = MaterialTheme.typography.titleMedium)
                Text(text = stringResource(resource = if (step == Step.Cable) Res.string.mobile_step_cable else Res.string.mobile_step_ecu),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        navigationIcon = {
            if (step == Step.Ecu) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(resource = Res.drawable.ic_arrow_back), contentDescription = stringResource(resource = Res.string.back_button))
                }
            }
        },
        actions = {
            TextButton(onClick = onLogs) { Text(stringResource(Res.string.live_log_open)) }

        },
    )
}

@Preview(name = "Подключение · App bar", showBackground = true, widthDp = 360)
@Composable
private fun ConnectionTopBarPreview() {
    DMELabTheme { ConnectionTopBar(step = Step.Ecu, onBack = {}, onLogs = {}) }
}
