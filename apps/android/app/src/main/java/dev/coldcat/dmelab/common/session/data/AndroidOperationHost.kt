package dev.coldcat.dmelab.common.session.data

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dev.coldcat.dmelab.common.session.domain.OperationHost
import dev.coldcat.dmelab.common.session.service.EcuOperationService
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

@Single
internal class AndroidOperationHost(@Provided private val context: Context) : OperationHost {
    override fun start() {
        ContextCompat.startForegroundService(context, Intent(context, EcuOperationService::class.java))
    }
}
