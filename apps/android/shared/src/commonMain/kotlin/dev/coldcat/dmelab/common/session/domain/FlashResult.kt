package dev.coldcat.dmelab.common.session.domain

import dev.coldcat.dmelab.common.ecu.model.FirmwareMode

data class FlashResult(
    val kind: OperationKind,
    val mode: FirmwareMode,
    val location: String,
    val byteCount: Int,
    val completedAt: Long,
    val elapsedMillis: Long = 0,
)
