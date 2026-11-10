package dev.coldcat.dmelab.features.diagnostics.dtc.presentation
internal sealed interface DtcScreenCommand {
    data object Back : DtcScreenCommand
}
