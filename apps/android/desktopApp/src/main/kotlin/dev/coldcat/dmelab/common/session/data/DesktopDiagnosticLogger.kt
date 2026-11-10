package dev.coldcat.dmelab.common.session.data

import dev.coldcat.dmelab.common.session.domain.DiagnosticLogger
import org.koin.core.annotation.Single

@Single
internal class DesktopDiagnosticLogger : DiagnosticLogger {
    override fun error(message: String) {
        System.err.println("DmeLabConnection: $message")
    }
}
