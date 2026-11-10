package dev.coldcat.dmelab.features.hub.ui

import dev.coldcat.dmelab.features.hub.presentation.HubScenario
import dev.coldcat.dmelab.features.hub.presentation.HubScreenState
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.hub_dtc
import dev.coldcat.dmelab.shared.resources.hub_dtc_description
import dev.coldcat.dmelab.shared.resources.hub_live_data
import dev.coldcat.dmelab.shared.resources.hub_live_data_description
import dev.coldcat.dmelab.shared.resources.hub_read_flash
import dev.coldcat.dmelab.shared.resources.hub_read_flash_description
import dev.coldcat.dmelab.shared.resources.hub_write_flash
import dev.coldcat.dmelab.shared.resources.hub_write_flash_description

/** Static fixtures used only by Compose previews; no session or DI dependencies. */
internal object HubPreviewData {
    val information = HubScreenState.Information(
        hardwareId = "MS43 preview",
        flashIdentifier = "AM29F400BB",
        firmwareVersion = "430069",
    )
    val scenarios = listOf(
        HubScreenState.Scenario(
            id = HubScenario.ReadFlash,
            title = Res.string.hub_read_flash,
            description = Res.string.hub_read_flash_description,
        ),
        HubScreenState.Scenario(
            id = HubScenario.WriteFlash,
            title = Res.string.hub_write_flash,
            description = Res.string.hub_write_flash_description,
        ),
        HubScreenState.Scenario(
            id = HubScenario.ReadDtc,
            title = Res.string.hub_dtc,
            description = Res.string.hub_dtc_description,
        ),
        HubScreenState.Scenario(
            id = HubScenario.LiveData,
            title = Res.string.hub_live_data,
            description = Res.string.hub_live_data_description,
        ),
    )
    val state = HubScreenState(information = information, scenarios = scenarios)
}
