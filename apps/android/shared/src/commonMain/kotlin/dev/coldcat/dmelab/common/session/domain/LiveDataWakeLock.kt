package dev.coldcat.dmelab.common.session.domain

interface LiveDataWakeLock {
    suspend fun hold(action: suspend () -> Unit)
}
