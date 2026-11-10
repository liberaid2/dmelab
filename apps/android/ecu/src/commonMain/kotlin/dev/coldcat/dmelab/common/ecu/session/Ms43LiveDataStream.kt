package dev.coldcat.dmelab.common.ecu.session

import dev.coldcat.dmelab.common.ecu.model.LiveDataSelection
import dev.coldcat.dmelab.common.ecu.model.SelectedLiveData

/**
 * One operation-scoped stream; stop remains available while the client is busy.
 * Invoke run and stop on the operation owner's dispatcher. Sample callbacks run
 * on the ECU worker and must complete before the next sample can be requested.
 */
interface Ms43LiveDataStream {
    fun stop()
    suspend fun run(
        adapterId: String,
        timeoutMs: UInt,
        selection: LiveDataSelection,
        onSample: suspend (SelectedLiveData) -> Unit,
        onRecovering: (ULong) -> Unit,
    )
}
