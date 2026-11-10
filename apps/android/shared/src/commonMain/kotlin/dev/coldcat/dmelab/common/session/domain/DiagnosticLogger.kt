package dev.coldcat.dmelab.common.session.domain

/** Diagnostic messages exclude ECU identifiers, firmware contents and live samples. */
interface DiagnosticLogger {
    fun error(message: String)
}
