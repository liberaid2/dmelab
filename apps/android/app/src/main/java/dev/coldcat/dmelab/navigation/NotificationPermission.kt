package dev.coldcat.dmelab.navigation

import android.content.pm.PackageManager
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.withStarted
import java.io.IOException
import kotlinx.coroutines.flow.first
import org.koin.compose.koinInject

/** Notification visibility is optional; the operation never waits for this permission. */
@Composable
internal fun NotificationPermission(hasActiveOperation: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val preferences = koinInject<DataStore<Preferences>>()
    val askedKey = booleanPreferencesKey(name = "operation_notification_permission_asked")
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(hasActiveOperation, lifecycle) {
        if (!hasActiveOperation || asked) return@LaunchedEffect
        val previouslyAsked = try { preferences.data.first()[askedKey] == true }
        catch (_: IOException) { return@LaunchedEffect }
        if (previouslyAsked) {
            asked = true
            return@LaunchedEffect
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return@LaunchedEffect
        }
        lifecycle.withStarted {
            asked = true
            launcher.launch(input = Manifest.permission.POST_NOTIFICATIONS)
        }
        try { preferences.edit { it[askedKey] = true } }
        catch (_: IOException) { /* Optional prompt remains remembered for this Activity. */ }
    }
}
