package dev.coldcat.dmelab.features.diagnostics.presentation

import dev.coldcat.dmelab.common.presentation.formatLocalTime
import dev.coldcat.dmelab.common.session.domain.OperationKind
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.diagnostics_clearing_dtc
import dev.coldcat.dmelab.shared.resources.diagnostics_polling
import dev.coldcat.dmelab.shared.resources.diagnostics_reading_dtc
import dev.coldcat.dmelab.shared.resources.diagnostics_reading_flash
import dev.coldcat.dmelab.shared.resources.diagnostics_writing_flash
import org.jetbrains.compose.resources.StringResource

internal object DiagnosticMessages {
    fun operation(value: OperationKind): StringResource = when (value) {
        OperationKind.ReadDtc -> Res.string.diagnostics_reading_dtc
        OperationKind.ClearDtc -> Res.string.diagnostics_clearing_dtc
        OperationKind.LiveData -> Res.string.diagnostics_polling
        OperationKind.ReadFlash -> Res.string.diagnostics_reading_flash
        OperationKind.WriteFlash -> Res.string.diagnostics_writing_flash
    }

    fun time(value: Long): String = formatLocalTime(epochMillis = value)
}
