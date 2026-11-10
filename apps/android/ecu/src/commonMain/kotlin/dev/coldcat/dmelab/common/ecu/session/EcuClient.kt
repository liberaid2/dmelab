package dev.coldcat.dmelab.common.ecu.session

import dev.coldcat.dmelab.common.ecu.model.Dtc
import dev.coldcat.dmelab.common.ecu.model.EcuInformation
import dev.coldcat.dmelab.common.ecu.model.FirmwareMode
import dev.coldcat.dmelab.common.ecu.model.LiveDataSelection
import dev.coldcat.dmelab.common.ecu.model.SelectedLiveData

/** Blocking operations, callable exclusively inside Ms43Access.use on its worker. */
interface EcuClient {
    fun identify(timeoutMs: UInt): EcuInformation
    fun readDtcs(timeoutMs: UInt): List<Dtc>
    fun clearDtcs(timeoutMs: UInt)
    fun readLiveDataSelected(selection: LiveDataSelection, timeoutMs: UInt): SelectedLiveData
    fun readFlash(mode: FirmwareMode, timeoutMs: UInt, listener: ProgressListener): ByteArray
    fun writeFlash(mode: FirmwareMode, image: ByteArray, timeoutMs: UInt, listener: ProgressListener)
}
