package dev.coldcat.dmelab

import dev.coldcat.dmelab.common.di.DesktopModule
import org.koin.core.annotation.KoinApplication

@KoinApplication(modules = [DesktopModule::class])
internal class DesktopApplication
