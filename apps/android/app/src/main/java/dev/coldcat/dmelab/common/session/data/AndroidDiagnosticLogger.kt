package dev.coldcat.dmelab.common.session.data

import android.util.Log
import dev.coldcat.dmelab.common.session.domain.DiagnosticLogger
import org.koin.core.annotation.Single

@Single
internal class AndroidDiagnosticLogger : DiagnosticLogger {
    override fun error(message: String) { Log.e("DmeLabConnection", message) }
}
