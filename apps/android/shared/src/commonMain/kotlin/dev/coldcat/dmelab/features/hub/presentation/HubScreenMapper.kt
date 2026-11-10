package dev.coldcat.dmelab.features.hub.presentation

import dev.coldcat.dmelab.common.session.domain.EcuConnectionState.Operation
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.hub_dtc
import dev.coldcat.dmelab.shared.resources.hub_dtc_description
import dev.coldcat.dmelab.shared.resources.hub_live_data
import dev.coldcat.dmelab.shared.resources.hub_live_data_description
import dev.coldcat.dmelab.shared.resources.hub_read_flash
import dev.coldcat.dmelab.shared.resources.hub_read_flash_description
import dev.coldcat.dmelab.shared.resources.hub_write_flash
import dev.coldcat.dmelab.shared.resources.hub_write_flash_description
import org.koin.core.annotation.Factory

@Factory
internal class HubScreenMapper {
    fun getScreenState(internal: HubInternalState): HubScreenState {
        val information = (internal.connection.operation as? Operation.Identified)?.information
        val scenarios = HubScenario.entries.map { scenario ->
            when (scenario) {
                HubScenario.ReadFlash -> HubScreenState.Scenario(id = scenario, title = Res.string.hub_read_flash, description = Res.string.hub_read_flash_description)
                HubScenario.WriteFlash -> HubScreenState.Scenario(id = scenario, title = Res.string.hub_write_flash, description = Res.string.hub_write_flash_description)
                HubScenario.ReadDtc -> HubScreenState.Scenario(id = scenario, title = Res.string.hub_dtc, description = Res.string.hub_dtc_description)
                HubScenario.LiveData -> HubScreenState.Scenario(id = scenario, title = Res.string.hub_live_data, description = Res.string.hub_live_data_description)
            }
        }
        return HubScreenState(
            information = information?.let {
                HubScreenState.Information(hardwareId = it.hardwareId, flashIdentifier = it.flashIdentifier, firmwareVersion = it.firmwareVersion)
            },
            scenarios = scenarios,
            canChangeConnection = !internal.busy,
        )
    }
}
