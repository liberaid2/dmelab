package dev.coldcat.dmelab.common.session.domain

import dev.coldcat.dmelab.common.ecu.model.FirmwareMode
import dev.coldcat.dmelab.common.ecu.model.LiveDataSelection
import kotlinx.coroutines.flow.StateFlow

internal interface EcuOperations {
    val state: StateFlow<EcuOperationState>
    fun readDtcs(): Boolean
    fun clearDtcs(expectedSessionId: Long): Boolean
    fun readLiveData(selection: LiveDataSelection = LiveDataSelection()): Boolean
    fun startLiveData(record: Boolean = false, selection: LiveDataSelection = LiveDataSelection()): Boolean
    fun stopLiveData()
    fun readFlash(mode: FirmwareMode, destination: String): Boolean
    fun writeFlash(mode: FirmwareMode, source: String, expectedSessionId: Long, expectedFingerprint: String): Boolean
}
