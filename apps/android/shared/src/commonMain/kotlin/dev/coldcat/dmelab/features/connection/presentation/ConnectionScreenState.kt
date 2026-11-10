package dev.coldcat.dmelab.features.connection.presentation

import org.jetbrains.compose.resources.StringResource

internal data class ConnectionScreenState(
    val step: Step,
    val title: StringResource,
    val description: StringResource,
    val adapters: List<AdapterItem>,
    val selectedId: String?,
    val notice: Notice,
    val primary: PrimaryAction,
    val information: EcuCard?,
    val isBusy: Boolean,
) {
    enum class Step { Cable, Ecu }
    enum class Tone { Neutral, Success, Attention, Error }
    data class AdapterItem(val id: String, val name: String, val description: String)
    data class Notice(val title: StringResource, val description: StringResource, val tone: Tone)
    data class PrimaryAction(val label: StringResource, val event: ConnectionScreenEvent?, val isLoading: Boolean)
    data class EcuCard(val hardwareId: String, val flashIdentifier: String, val firmwareVersion: String)
}
