package dev.coldcat.dmelab.common.di

import dev.coldcat.dmelab.common.resources.AppStrings
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan("dev.coldcat.dmelab")
internal class DesktopModule {
    @Single
    @Named("io")
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Single
    @Named("application")
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Single
    @Named("ecu")
    fun ecuDispatcher(): CoroutineDispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "dmelab-ecu")
    }.asCoroutineDispatcher()

    @Single
    fun strings(): AppStrings = runBlocking(context = Dispatchers.IO) { AppStrings.load() }
}
