package dev.coldcat.dmelab.navigation

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import dev.coldcat.dmelab.common.ecu.model.FirmwareMode
import dev.coldcat.dmelab.common.navigation.Destination
import dev.coldcat.dmelab.common.navigation.Router
import dev.coldcat.dmelab.features.connection.ui.ConnectionScreen
import dev.coldcat.dmelab.features.diagnostics.dtc.ui.DtcScreen
import dev.coldcat.dmelab.features.diagnostics.livedata.ui.LiveDataScreen
import dev.coldcat.dmelab.features.firmware.read.ui.ReadFlashScreen
import dev.coldcat.dmelab.features.firmware.write.ui.WriteFlashScreen
import dev.coldcat.dmelab.features.hub.ui.HubScreen

/** Feature implementations are resolved at the application composition boundary. */
internal class VoyagerRouter(private val navigator: Navigator) : Router {
    override fun setRoot(destination: Destination) {
        if (navigator.items.size != 1 || navigator.lastItem.destination() != destination) {
            navigator.replaceAll(destination.screen())
        }
    }

    override fun open(destination: Destination) {
        if (navigator.lastItem.destination() == destination) return
        val previous = navigator.items.firstOrNull { it.destination() == destination }
        if (previous != null) navigator.popUntil { it.key == previous.key }
        else navigator.push(destination.screen())
    }

    override fun back() { navigator.pop() }

    override fun openFirmware(source: String, mode: FirmwareMode) {
        val previous = navigator.items.firstOrNull { it is WriteFlashScreen }
        if (previous != null) {
            navigator.popUntil { it.key == previous.key }
            navigator.replace(WriteFlashScreen(initialFile = source, initialMode = mode))
        } else navigator.push(WriteFlashScreen(initialFile = source, initialMode = mode))
    }
}

internal fun Screen.destination(): Destination = when (this) {
    is ConnectionScreen -> Destination.Connection
    is HubScreen -> Destination.Hub
    is ReadFlashScreen -> Destination.ReadFlash
    is WriteFlashScreen -> Destination.WriteFlash
    is DtcScreen -> Destination.Dtc
    is LiveDataScreen -> Destination.LiveData
    else -> error("Unknown screen: ${this::class.simpleName}")
}

private fun Destination.screen(): Screen = when (this) {
    Destination.Connection -> ConnectionScreen()
    Destination.Hub -> HubScreen()
    Destination.ReadFlash -> ReadFlashScreen()
    Destination.WriteFlash -> WriteFlashScreen()
    Destination.Dtc -> DtcScreen()
    Destination.LiveData -> LiveDataScreen()
}
