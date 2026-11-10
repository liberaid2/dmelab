package dev.coldcat.dmelab

import android.app.Application
import dev.coldcat.dmelab.common.di.AppModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.annotation.KoinApplication
import org.koin.plugin.module.dsl.startKoin

@KoinApplication(modules = [AppModule::class])
class DmeLabApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin<DmeLabApplication> {
            androidContext(this@DmeLabApplication)
        }
    }
}
