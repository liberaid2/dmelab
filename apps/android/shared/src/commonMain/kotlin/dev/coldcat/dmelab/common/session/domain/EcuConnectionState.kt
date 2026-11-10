package dev.coldcat.dmelab.common.session.domain

import dev.coldcat.dmelab.common.ecu.model.EcuInformation
import dev.coldcat.dmelab.common.ecu.model.UsbInventory
internal data class EcuConnectionState(
    val inventory: UsbInventory,
    val selectedId: String?,
    val operation: Operation,
    val sessionId: Long = 0,
) {
    sealed interface Operation {
        data object Idle : Operation
        data object Identifying : Operation
        data class Identified(val information: EcuInformation) : Operation
        data class Failed(val problem: ConnectionProblem) : Operation
    }
}
