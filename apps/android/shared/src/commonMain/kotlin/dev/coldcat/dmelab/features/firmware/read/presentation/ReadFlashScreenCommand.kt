package dev.coldcat.dmelab.features.firmware.read.presentation

internal sealed interface ReadFlashScreenCommand {
    data object Back : ReadFlashScreenCommand
    data class ChooseDestination(val name: String) : ReadFlashScreenCommand
}
