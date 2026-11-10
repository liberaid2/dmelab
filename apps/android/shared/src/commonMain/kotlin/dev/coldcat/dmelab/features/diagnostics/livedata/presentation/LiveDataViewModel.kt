package dev.coldcat.dmelab.features.diagnostics.livedata.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.coldcat.dmelab.common.ecu.model.LiveDataSelection
import dev.coldcat.dmelab.common.session.domain.EcuConnection
import dev.coldcat.dmelab.common.session.domain.EcuOperations
import dev.coldcat.dmelab.common.session.domain.LiveDataLogName
import dev.coldcat.dmelab.common.session.domain.LiveDataLogs
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.live_log_archive_saved
import dev.coldcat.dmelab.shared.resources.live_log_file_error
import dev.coldcat.dmelab.shared.resources.live_log_rename_error
import dev.coldcat.dmelab.shared.resources.live_log_renamed
import dev.coldcat.dmelab.shared.resources.live_log_saved
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class LiveDataViewModel(
    connection: EcuConnection,
    private val operations: EcuOperations,
    mapper: LiveDataScreenMapper,
    private val logs: LiveDataLogs,
) : ViewModel() {
    private val internalState = MutableStateFlow(LiveDataInternalState(
        connection = connection.state.value,
        operations = operations.state.value,
        selection = operations.state.value.active?.liveDataSelection ?: LiveDataSelection(),
    ))
    private val commandChannel = Channel<LiveDataScreenCommand>(capacity = Channel.BUFFERED)
    val commands = commandChannel.receiveAsFlow()
    val state = internalState.map(mapper::getScreenState).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = mapper.getScreenState(internal = internalState.value),
    )

    init {
        viewModelScope.launch {
            logs.logs.collect { values ->
                val available = values.filter { !it.recording && it.available }.map { it.id }.toSet()
                internalState.update { it.copy(logs = values, selectedLogIds = it.selectedLogIds.intersect(available)) }
            }
        }
        fileAction { logs.load() }
        viewModelScope.launch {
            combine(connection.state, operations.state) { session, operation -> session to operation }
                .collect { (session, operation) ->
                    internalState.update { it.copy(connection = session, operations = operation,
                        selection = operation.active?.liveDataSelection ?: it.selection) }
                }
        }
    }

    fun onEvent(event: LiveDataScreenEvent) {
        when (event) {
            is LiveDataScreenEvent.OnGroupChanged -> if (operations.state.value.active == null) {
                internalState.update { it.copy(selection = it.selection.withGroup(group = event.group, enabled = event.enabled)) }
            }
            is LiveDataScreenEvent.OnRecordChanged -> if (operations.state.value.active == null) {
                internalState.update { it.copy(record = event.enabled) }
            }
            is LiveDataScreenEvent.OnShare -> fileAction { share(target = LiveDataExportTarget.Csv(id = event.id)) }
            is LiveDataScreenEvent.OnToggleSelection -> toggleSelection(id = event.id)
            LiveDataScreenEvent.OnShareSelected -> {
                val snapshot = internalState.value
                val ids = snapshot.logs.filter { it.id in snapshot.selectedLogIds }.map { it.id }
                if (ids.isNotEmpty()) fileAction {
                    val uris = logs.shareUris(ids = ids)
                    commandChannel.send(LiveDataScreenCommand.Share(uris = uris, format = LiveDataExportTarget.Format.Csv))
                }
            }
            is LiveDataScreenEvent.OnSave -> if (!internalState.value.fileBusy) {
                chooseDestination(target = LiveDataExportTarget.Csv(id = event.id))
            }
            LiveDataScreenEvent.OnShareAll -> fileAction {
                val archive = logs.createArchive()
                share(target = LiveDataExportTarget.Archive(id = archive.id))
            }
            LiveDataScreenEvent.OnSaveAll -> fileAction {
                val archive = logs.createArchive()
                chooseDestination(target = LiveDataExportTarget.Archive(id = archive.id))
            }
            is LiveDataScreenEvent.OnDestination -> exportTo(destination = event.uri)
            is LiveDataScreenEvent.OnRename -> openRename(id = event.id)
            is LiveDataScreenEvent.OnRenameNameChanged -> if (!internalState.value.fileBusy) {
                internalState.update { it.copy(rename = it.rename?.copy(name = event.name, failure = null)) }
            }
            LiveDataScreenEvent.OnConfirmRename -> renameLog()
            LiveDataScreenEvent.OnDismissRename -> if (!internalState.value.fileBusy) {
                internalState.update { it.copy(rename = null) }
            }
            is LiveDataScreenEvent.OnDelete -> internalState.update { it.copy(deleteId = event.id) }
            LiveDataScreenEvent.OnDismissDelete -> internalState.update { it.copy(deleteId = null) }
            LiveDataScreenEvent.OnConfirmDelete -> {
                val id = internalState.value.deleteId
                internalState.update { it.copy(deleteId = null) }
                if (id != null) fileAction { logs.delete(id = id) }
            }
            LiveDataScreenEvent.OnToggleLogList -> if (internalState.value.selectedLogIds.isEmpty()) {
                internalState.update { it.copy(showAllLogs = !it.showAllLogs) }
            }
            LiveDataScreenEvent.OnReloadLogs -> fileAction { logs.load() }
            LiveDataScreenEvent.OnFileFailure -> internalState.update {
                it.copy(fileMessage = Res.string.live_log_file_error, fileBusy = false, exportTarget = null)
            }
            LiveDataScreenEvent.OnBack -> if (internalState.value.rename != null) {
                if (!internalState.value.fileBusy) internalState.update { it.copy(rename = null) }
            } else if (internalState.value.selectedLogIds.isNotEmpty()) {
                internalState.update { it.copy(selectedLogIds = emptySet()) }
            } else { commandChannel.trySend(LiveDataScreenCommand.Back) }
            LiveDataScreenEvent.OnRead -> operations.readLiveData(selection = internalState.value.selection)
            LiveDataScreenEvent.OnStart -> operations.startLiveData(record = internalState.value.record, selection = internalState.value.selection)
            LiveDataScreenEvent.OnStop -> operations.stopLiveData()
        }
    }

    private fun openRename(id: String) {
        val current = internalState.value
        if (current.fileBusy || current.selectedLogIds.isNotEmpty()) return
        val log = current.logs.firstOrNull { it.id == id && !it.recording && it.available } ?: return
        val stem = log.id.removeSuffix(".csv").let { if (log.partial) it.removeSuffix("_partial") else it }
        internalState.update { it.copy(rename = LiveDataInternalState.Rename(id = id, name = stem, partial = log.partial),
            deleteId = null, fileMessage = null) }
    }

    private fun renameLog() {
        val pending = internalState.value.rename ?: return
        if (LiveDataLogName.stem(input = pending.name, partial = pending.partial) == null) return
        fileAction {
            logs.rename(id = pending.id, name = pending.name)
            internalState.update { it.copy(rename = null, fileMessage = Res.string.live_log_renamed) }
        }
    }

    private fun toggleSelection(id: String) {
        internalState.update { current ->
            if (current.fileBusy || current.logs.none { it.id == id && it.available && !it.recording }) current
            else current.copy(
                selectedLogIds = if (id in current.selectedLogIds) current.selectedLogIds - id else current.selectedLogIds + id,
                showAllLogs = true,
                deleteId = null,
            )
        }
    }

    private suspend fun share(target: LiveDataExportTarget) {
        val uri = when (target) {
            is LiveDataExportTarget.Csv -> logs.shareUri(id = target.id)
            is LiveDataExportTarget.Archive -> logs.shareArchiveUri(id = target.id)
        }
        commandChannel.send(LiveDataScreenCommand.Share(uris = listOf(uri), format = target.format))
    }

    private fun chooseDestination(target: LiveDataExportTarget) {
        internalState.update { it.copy(fileBusy = true, fileMessage = null, exportTarget = target) }
        commandChannel.trySend(LiveDataScreenCommand.ChooseDestination(name = target.id, format = target.format))
    }

    private fun exportTo(destination: String?) {
        val target = internalState.value.exportTarget
        internalState.update { it.copy(fileBusy = false, exportTarget = null) }
        if (target == null && destination != null) fileFailure()
        if (target != null && destination != null) fileAction {
            when (target) {
                is LiveDataExportTarget.Csv -> logs.export(id = target.id, destination = destination)
                is LiveDataExportTarget.Archive -> logs.exportArchive(id = target.id, destination = destination)
            }
            internalState.update { it.copy(fileMessage = when (target) {
                is LiveDataExportTarget.Csv -> Res.string.live_log_saved
                is LiveDataExportTarget.Archive -> Res.string.live_log_archive_saved
            }) }
        }
    }

    private fun fileAction(action: suspend () -> Unit) {
        if (internalState.value.fileBusy) return
        internalState.update { it.copy(fileBusy = true, fileMessage = null) }
        viewModelScope.launch {
            try { action() }
            catch (exception: CancellationException) { throw exception }
            catch (_: Exception) { fileFailure() }
            finally { internalState.update { it.copy(fileBusy = it.exportTarget != null) } }
        }
    }

    private fun fileFailure() {
        internalState.update { it.copy(
            fileMessage = if (it.rename == null) Res.string.live_log_file_error else null,
            rename = it.rename?.copy(failure = Res.string.live_log_rename_error),
        ) }
    }
}
