package dev.coldcat.dmelab.common.platform

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberFileSharer(onFailure: () -> Unit): FileSharer {
    val context = LocalContext.current
    val failure = rememberUpdatedState(onFailure)
    return remember(context) {
        FileSharer { locations, mimeType ->
            val uris = ArrayList(locations.map(Uri::parse))
            if (uris.isEmpty()) {
                failure.value()
            } else {
                val multiple = uris.size > 1
                val intent = Intent(if (multiple) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND).apply {
                    type = mimeType
                    if (multiple) putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    else putExtra(Intent.EXTRA_STREAM, uris.first())
                    clipData = ClipData.newRawUri("Log export", uris.first()).apply {
                        uris.drop(1).forEach { addItem(ClipData.Item(it)) }
                    }
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try { context.startActivity(Intent.createChooser(intent, null)) }
                catch (_: ActivityNotFoundException) { failure.value() }
                catch (_: SecurityException) { failure.value() }
            }
        }
    }
}
