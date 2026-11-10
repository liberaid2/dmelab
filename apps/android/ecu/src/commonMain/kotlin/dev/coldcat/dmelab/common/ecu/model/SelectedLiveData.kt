package dev.coldcat.dmelab.common.ecu.model

data class SelectedLiveData(
    val measurements: LiveMeasurements?,
    val vanos: LiveVanos?,
    val digital: LiveDigital?,
)
