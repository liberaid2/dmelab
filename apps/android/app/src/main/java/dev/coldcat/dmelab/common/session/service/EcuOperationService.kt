package dev.coldcat.dmelab.common.session.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dev.coldcat.dmelab.common.session.domain.OperationFailure
import dev.coldcat.dmelab.common.session.domain.OperationKind
import dev.coldcat.dmelab.common.session.domain.OperationRuntime
import dev.coldcat.dmelab.MainActivity
import dev.coldcat.dmelab.R
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.android.inject

/** No restart/redelivery: flash must never resume automatically after process death. */
internal class EcuOperationService : Service() {
    private val coordinator: OperationRuntime by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.operation_notification_channel), NotificationManager.IMPORTANCE_LOW))
        if (!promote()) return
        scope.launch {
            coordinator.state.collect { if (it.active != null) manager.notify(NOTIFICATION_ID, notification()) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!promote()) return START_NOT_STICKY
        val job = coordinator.runPending(scope = scope)
        if (job == null) {
            if (coordinator.state.value.active == null) finish(startId = startId)
        } else {
            job.invokeOnCompletion { finish(startId = startId) }
        }
        return START_NOT_STICKY
    }

    private fun promote(): Boolean = try {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(), type)
        true
    } catch (exception: RuntimeException) {
        coordinator.rejectPending(problem = if (exception is SecurityException) OperationFailure.Permission else OperationFailure.Unexpected)
        stopSelf()
        false
    }

    private fun finish(startId: Int) {
        if (coordinator.state.value.active == null && stopSelfResult(startId)) stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun notification(): android.app.Notification {
        val active = coordinator.state.value.active
        val label = when (active?.kind) {
            OperationKind.ReadDtc -> R.string.operation_read_dtc
            OperationKind.ClearDtc -> R.string.operation_clear_dtc
            OperationKind.LiveData -> R.string.operation_live
            OperationKind.ReadFlash -> R.string.operation_read_flash
            OperationKind.WriteFlash -> R.string.operation_write_flash
            null -> R.string.operation_notification_starting
        }
        val launchIntent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(this, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val progress = active?.progress
        val percentage = if (progress != null && progress.total > 0) ((progress.completed.coerceIn(0, progress.total) * 100) / progress.total).toInt() else null
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_ecu_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(label))
            .setContentIntent(pending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, percentage ?: 0, percentage == null)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object { const val CHANNEL = "ecu_operations"; const val NOTIFICATION_ID = 43 }
}
