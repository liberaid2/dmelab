package dev.coldcat.dmelab.common.session.data

import android.content.Context
import android.os.PowerManager
import dev.coldcat.dmelab.common.session.domain.LiveDataWakeLock
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/** CPU protection scoped to the service-owned polling job, including CSV finalization. */
@Single
internal class AndroidLiveDataWakeLock(@Provided private val context: Context) : LiveDataWakeLock {
    override suspend fun hold(action: suspend () -> Unit): Unit = coroutineScope {
        val power = context.getSystemService(PowerManager::class.java)
        val lock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
        lock.setReferenceCounted(false)
        try {
            lock.acquire(LEASE_MILLIS)
            // A finite lease limits leaks; renew only while the polling job is alive.
            val renewal = launch {
                while (isActive) {
                    delay(timeMillis = RENEWAL_MILLIS)
                    lock.acquire(LEASE_MILLIS)
                }
            }
            try {
                action()
            } finally {
                renewal.cancel()
            }
        } finally {
            // The native call returns and CSV cleanup finishes before action exits,
            // including when service destruction cancels the operation.
            if (lock.isHeld) lock.release()
        }
    }

    private companion object {
        const val WAKE_LOCK_TAG = "dev.coldcat.dmelab:LiveDataPolling"
        const val LEASE_MILLIS = 10 * 60 * 1000L
        const val RENEWAL_MILLIS = 5 * 60 * 1000L
    }
}
