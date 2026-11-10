package dev.coldcat.dmelab.common.session.domain

import dev.coldcat.dmelab.common.ecu.model.LiveDataSelection
import dev.coldcat.dmelab.common.ecu.model.SelectedLiveData
import kotlinx.coroutines.flow.StateFlow

interface LiveDataLogs {
    val logs: StateFlow<List<LiveDataLog>>
    suspend fun start(selection: LiveDataSelection)
    suspend fun append(data: SelectedLiveData, timestampMillis: Long, completedAtMillis: Long)
    suspend fun finish(partial: Boolean)
    suspend fun load()
    suspend fun shareUri(id: String): String
    suspend fun shareUris(ids: List<String>): List<String>
    suspend fun export(id: String, destination: String)
    suspend fun createArchive(): LiveDataLogArchive
    suspend fun shareArchiveUri(id: String): String
    suspend fun exportArchive(id: String, destination: String)
    suspend fun delete(id: String)
    suspend fun rename(id: String, name: String): LiveDataLog
}
