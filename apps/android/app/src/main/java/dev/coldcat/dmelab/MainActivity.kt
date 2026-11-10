package dev.coldcat.dmelab

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.features.firmware.open.presentation.FirmwareOpenViewModel
import dev.coldcat.dmelab.navigation.NotificationPermission
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val firmwareOpen: FirmwareOpenViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(incoming = intent, newRequest = false)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(scrim = Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(scrim = Color.TRANSPARENT),
        )
        setContent {
            DMELabTheme {
                DmeLabApp(firmwareOpen = firmwareOpen, notificationPermission = { active -> NotificationPermission(hasActiveOperation = active) })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(incoming = intent, newRequest = true)
    }

    private fun handleIntent(incoming: Intent, newRequest: Boolean) {
        if (incoming.action == Intent.ACTION_VIEW) {
            incoming.data?.let { uri ->
                firmwareOpen.open(location = uri.toString(), newRequest = newRequest)
            }
        }
        // USB launch extras are not trusted; the existing ON_RESUME handler re-enumerates UsbManager.
    }
}

