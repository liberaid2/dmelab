package dev.coldcat.dmelab.features.firmware.write.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.firmware_back
import dev.coldcat.dmelab.shared.resources.firmware_write_title
import dev.coldcat.dmelab.shared.resources.ic_arrow_back
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WriteFlashTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text(text = stringResource(resource = Res.string.firmware_write_title), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(painter = painterResource(resource = Res.drawable.ic_arrow_back), contentDescription = stringResource(resource = Res.string.firmware_back))
            }
        },
    )
}

@Preview(name = "Панель навигации", showBackground = true, widthDp = 360)
@Composable
private fun TopBarPreview() {
    DMELabTheme { WriteFlashTopBar(onBack = {}) }
}
