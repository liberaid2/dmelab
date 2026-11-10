package dev.coldcat.dmelab.common.platform

import android.os.SystemClock

internal actual fun monotonicTimeNanos(): Long = SystemClock.elapsedRealtimeNanos()
