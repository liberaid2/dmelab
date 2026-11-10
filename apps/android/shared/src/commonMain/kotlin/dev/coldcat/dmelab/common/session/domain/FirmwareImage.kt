package dev.coldcat.dmelab.common.session.domain

import dev.coldcat.dmelab.common.ecu.model.FirmwareMode

data class FirmwareImage(val location: String, val name: String, val byteCount: Int, val mode: FirmwareMode, val fingerprint: String)
