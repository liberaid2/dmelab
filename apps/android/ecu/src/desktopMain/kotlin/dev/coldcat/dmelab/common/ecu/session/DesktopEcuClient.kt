package dev.coldcat.dmelab.common.ecu.session

import dev.coldcat.dmelab.common.ecu.model.Dtc
import dev.coldcat.dmelab.common.ecu.model.EcuInformation
import dev.coldcat.dmelab.common.ecu.model.FirmwareMode
import dev.coldcat.dmelab.common.ecu.model.LiveDataSelection
import dev.coldcat.dmelab.common.ecu.model.SelectedLiveData
import dev.coldcat.dmelab.ffi.BaudRate
import dev.coldcat.dmelab.ffi.Ms43Client

/** Operation-scoped facade; native lifetime belongs exclusively to Ms43Access. */
internal class DesktopEcuClient(internal val native: Ms43Client) : EcuClient {
    override fun identify(timeoutMs: UInt): EcuInformation = nativeCall {
        val information = native.identify(timeoutMs = timeoutMs)
        EcuInformation(
            hardwareId = information.hardwareId,
            flashIdentifier = information.flashIdentifier,
            firmwareVersion = information.firmwareVersion,
        )
    }

    override fun readDtcs(timeoutMs: UInt): List<Dtc> = nativeCall { native.readDtcs(timeoutMs = timeoutMs).map { it.toModel() } }

    override fun clearDtcs(timeoutMs: UInt) = nativeCall { native.clearDtcs(timeoutMs = timeoutMs) }

    override fun readLiveDataSelected(selection: LiveDataSelection, timeoutMs: UInt): SelectedLiveData =
        nativeCall { native.readLiveDataSelected(selection = selection.toNative(), timeoutMs = timeoutMs).toModel() }

    override fun readFlash(mode: FirmwareMode, timeoutMs: UInt, listener: ProgressListener): ByteArray =
        nativeCall { native.readFlash(mode = mode.toNative(), baudRate = BaudRate.B125000, timeoutMs = timeoutMs, listener = listener.toNative()) }

    override fun writeFlash(mode: FirmwareMode, image: ByteArray, timeoutMs: UInt, listener: ProgressListener) =
        nativeCall { native.writeFlash(mode = mode.toNative(), image = image, baudRate = BaudRate.B125000, timeoutMs = timeoutMs, listener = listener.toNative()) }
}

private fun ProgressListener.toNative(): dev.coldcat.dmelab.ffi.ProgressListener =
    object : dev.coldcat.dmelab.ffi.ProgressListener {
        override fun onProgress(progress: dev.coldcat.dmelab.ffi.Progress) = this@toNative.onProgress(progress.toModel())
    }
