package dev.coldcat.dmelab.features.diagnostics.dtc.presentation
internal sealed interface DtcScreenEvent {
    data object OnBack : DtcScreenEvent
    data object OnRead : DtcScreenEvent
    data object OnClear : DtcScreenEvent
    data object OnConfirmClear : DtcScreenEvent
    data object OnDismissClear : DtcScreenEvent
}
