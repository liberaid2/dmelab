package dev.coldcat.dmelab.common.ecu.session

import dev.coldcat.dmelab.common.ecu.model.EcuInformation
import org.koin.core.annotation.Single

/** Identification shares the same admission and resource owner as all other ECU calls. */
@Single
internal class AndroidIdentifyEcu(private val access: Ms43Access) : IdentifyEcu {
    override suspend fun invoke(adapterId: String): EcuInformation = access.use(adapterId = adapterId) { client ->
        client.identify(timeoutMs = IDENTIFY_TIMEOUT_MS)
    }

    private companion object {
        const val IDENTIFY_TIMEOUT_MS = 1500u
    }
}
