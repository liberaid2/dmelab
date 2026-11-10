package dev.coldcat.dmelab.common.session.domain

import dev.coldcat.dmelab.common.ecu.model.EcuInformation
internal data class EcuSessionLease(val adapterId: String, val sessionId: Long, val information: EcuInformation)
