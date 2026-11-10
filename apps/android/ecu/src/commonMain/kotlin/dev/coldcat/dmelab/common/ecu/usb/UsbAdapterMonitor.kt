package dev.coldcat.dmelab.common.ecu.usb

import dev.coldcat.dmelab.common.ecu.model.UsbInventory
import kotlinx.coroutines.flow.StateFlow

/** Platform adapter inventory; native USB objects never escape the implementation. */
interface UsbAdapterMonitor {
    val inventory: StateFlow<UsbInventory>
    fun refresh()
    fun requestPermission(id: String, force: Boolean)
}
