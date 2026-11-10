package dev.coldcat.dmelab.features.connection.presentation

import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.cable_footer
import dev.coldcat.dmelab.shared.resources.cable_phone_body
import dev.coldcat.dmelab.shared.resources.cable_phone_title
import dev.coldcat.dmelab.shared.resources.usb_choose_body
import dev.coldcat.dmelab.shared.resources.usb_waiting_body

internal actual fun connectionInstructions(): ConnectionInstructions = ConnectionInstructions(
    cableTitle = Res.string.cable_phone_title,
    cableBody = Res.string.cable_phone_body,
    waitingBody = Res.string.usb_waiting_body,
    chooseBody = Res.string.usb_choose_body,
    footer = Res.string.cable_footer,
)
