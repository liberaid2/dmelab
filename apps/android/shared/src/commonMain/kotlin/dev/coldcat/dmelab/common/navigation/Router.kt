package dev.coldcat.dmelab.common.navigation

import dev.coldcat.dmelab.common.ecu.model.FirmwareMode

internal interface Router {
    fun setRoot(destination: Destination)
    fun open(destination: Destination)
    fun openFirmware(source: String, mode: FirmwareMode)
    fun back()
}
