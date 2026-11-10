package dev.coldcat.dmelab.common.di

import dev.coldcat.dmelab.common.resources.AppStrings
import kotlinx.coroutines.runBlocking
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

@Module
@ComponentScan("dev.coldcat.dmelab")
internal class AppModule {
    // Resolve once on IO; synchronous screen mapping only reads this immutable catalog.
    @Single
    fun appStrings(): AppStrings = runBlocking(context = Dispatchers.IO) { AppStrings.load() }

    @Single
    @Named("io")
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Single
    @Named("application")
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Application-owned executor; Rust and USB callbacks run synchronously in this thread.
    @Single
    @Named("ecu")
    fun ecuDispatcher(): CoroutineDispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "dmelab-ecu")
    }.asCoroutineDispatcher()

    @Single
    fun preferences(@Provided context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(name = "settings") },
        )
}
