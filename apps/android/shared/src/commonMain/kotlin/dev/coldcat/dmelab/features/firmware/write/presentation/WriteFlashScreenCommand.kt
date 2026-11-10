package dev.coldcat.dmelab.features.firmware.write.presentation

internal sealed interface WriteFlashScreenCommand {
    data object Back : WriteFlashScreenCommand
    data object ChooseFile : WriteFlashScreenCommand
}
