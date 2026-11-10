package dev.coldcat.dmelab.features.firmware.read.presentation

import dev.coldcat.dmelab.common.presentation.formatLocalTime
import dev.coldcat.dmelab.common.session.domain.EcuConnectionState
import dev.coldcat.dmelab.common.session.domain.OperationFailure
import dev.coldcat.dmelab.common.session.domain.OperationKind
import dev.coldcat.dmelab.common.session.presentation.formatOperationDuration
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.firmware_busy
import dev.coldcat.dmelab.shared.resources.firmware_disconnected
import dev.coldcat.dmelab.shared.resources.firmware_exchange_error
import dev.coldcat.dmelab.shared.resources.firmware_file_error
import dev.coldcat.dmelab.shared.resources.firmware_interrupted
import dev.coldcat.dmelab.shared.resources.firmware_invalid_image
import dev.coldcat.dmelab.shared.resources.firmware_library_error
import dev.coldcat.dmelab.shared.resources.firmware_low_voltage
import dev.coldcat.dmelab.shared.resources.firmware_mismatch
import dev.coldcat.dmelab.shared.resources.firmware_reconnect
import dev.coldcat.dmelab.shared.resources.firmware_unexpected_error
import org.jetbrains.compose.resources.StringResource
import org.koin.core.annotation.Factory

@Factory
internal class ReadFlashScreenMapper {
    fun getScreenState(internal: ReadFlashInternalState): ReadFlashScreenState {
        val active = internal.operation.active
        val running = active?.kind == OperationKind.ReadFlash
        val progress = active?.progress.takeIf { running }
        val ready = internal.connection.operation is EcuConnectionState.Operation.Identified
        val belongsToCurrentEcu = !ready || internal.connection.sessionId == internal.operation.resultSessionId
        val result = internal.operation.flash?.takeIf { it.kind == OperationKind.ReadFlash && belongsToCurrentEcu }
        return ReadFlashScreenState(
            mode = internal.mode,
            canStart = ready && active == null,
            connectionRequired = !ready && active == null,
            busy = active != null,
            running = running,
            progress = progress?.takeIf { it.total > 0 }?.let { (it.completed.toFloat() / it.total).coerceIn(0f, 1f) },
            progressLabel = progress?.let { "${it.stage} · ${it.completed} / ${it.total}" },
            failure = (internal.localFailure ?: internal.operation.failure?.takeIf {
                it == OperationFailure.Interrupted ||
                    (belongsToCurrentEcu && internal.operation.failureKind == OperationKind.ReadFlash)
            })?.let(::failureResource),
            result = result?.let { "${formatLocalTime(epochMillis = it.completedAt, includeYear = true)} · ${it.byteCount} B\n${it.location}" },
            elapsed = active?.takeIf { running }?.let { formatOperationDuration(elapsedMillis = it.elapsedMillis) },
            resultDuration = result?.let { formatOperationDuration(elapsedMillis = it.elapsedMillis) },
            resultMode = result?.mode,
        )
    }

    private fun failureResource(problem: OperationFailure): StringResource = when (problem) {
        OperationFailure.Busy -> Res.string.firmware_busy
        OperationFailure.Disconnected, OperationFailure.Permission -> Res.string.firmware_disconnected
        OperationFailure.NeedsReconnect -> Res.string.firmware_reconnect
        OperationFailure.Exchange -> Res.string.firmware_exchange_error
        OperationFailure.InvalidImage -> Res.string.firmware_invalid_image
        OperationFailure.LowVoltage -> Res.string.firmware_low_voltage
        OperationFailure.FirmwareMismatch -> Res.string.firmware_mismatch
        OperationFailure.FileAccess -> Res.string.firmware_file_error
        OperationFailure.NativeLibrary -> Res.string.firmware_library_error
        OperationFailure.Unexpected -> Res.string.firmware_unexpected_error
        OperationFailure.Interrupted -> Res.string.firmware_interrupted
    }
}
