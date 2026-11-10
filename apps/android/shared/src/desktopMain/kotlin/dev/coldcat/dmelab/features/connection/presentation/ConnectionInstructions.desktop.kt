package dev.coldcat.dmelab.features.connection.presentation

import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.mac_cable_body
import dev.coldcat.dmelab.shared.resources.mac_cable_footer
import dev.coldcat.dmelab.shared.resources.mac_cable_title
import dev.coldcat.dmelab.shared.resources.mac_choose_port_body
import dev.coldcat.dmelab.shared.resources.mac_waiting_body

internal actual fun connectionInstructions(): ConnectionInstructions = ConnectionInstructions(
    cableTitle = Res.string.mac_cable_title,
    cableBody = Res.string.mac_cable_body,
    waitingBody = Res.string.mac_waiting_body,
    chooseBody = Res.string.mac_choose_port_body,
    footer = Res.string.mac_cable_footer,
)
