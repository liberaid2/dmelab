package dev.coldcat.dmelab.common.session.domain

interface OperationJournal {
    val wasRunning: Boolean
    suspend fun markRunning(running: Boolean): Boolean
}
