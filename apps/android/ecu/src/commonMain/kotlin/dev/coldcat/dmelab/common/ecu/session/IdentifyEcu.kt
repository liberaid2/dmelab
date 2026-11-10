package dev.coldcat.dmelab.common.ecu.session

import dev.coldcat.dmelab.common.ecu.model.EcuInformation

interface IdentifyEcu {
    suspend fun invoke(adapterId: String): EcuInformation
}
