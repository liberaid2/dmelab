package dev.coldcat.dmelab.common.ecu.model

data class LiveDataSelection(
    val measurements: Boolean = true,
    val vanos: Boolean = true,
    val digital: Boolean = true,
) {
    val isNotEmpty: Boolean get() = measurements || vanos || digital

    fun withGroup(group: LiveDataGroup, enabled: Boolean): LiveDataSelection = when (group) {
        LiveDataGroup.Measurements -> copy(measurements = enabled)
        LiveDataGroup.Vanos -> copy(vanos = enabled)
        LiveDataGroup.Digital -> copy(digital = enabled)
    }
}
