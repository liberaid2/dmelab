package dev.coldcat.dmelab.common.session.data

import dev.coldcat.dmelab.common.ecu.EcuException
import dev.coldcat.dmelab.common.session.domain.FirmwareFileException
import dev.coldcat.dmelab.common.session.domain.OperationFailure
import kotlinx.io.IOException
import dev.coldcat.dmelab.common.platform.isPermissionFailure
import dev.coldcat.dmelab.common.platform.isNativeLibraryFailure

object OperationErrors {
    fun from(error: Throwable): OperationFailure = when {
        isPermissionFailure(error) -> OperationFailure.Permission
        isNativeLibraryFailure(error) -> OperationFailure.NativeLibrary
        error is FirmwareFileException -> error.problem
        error is EcuException -> when (error.kind) {
            EcuException.Kind.Permission -> OperationFailure.Permission
            EcuException.Kind.NativeLibrary -> OperationFailure.NativeLibrary
            EcuException.Kind.Busy -> OperationFailure.Busy
            EcuException.Kind.NeedsReconnect -> OperationFailure.NeedsReconnect
            EcuException.Kind.InvalidImage -> OperationFailure.InvalidImage
            EcuException.Kind.LowVoltage -> OperationFailure.LowVoltage
            EcuException.Kind.FirmwareMismatch -> OperationFailure.FirmwareMismatch
            EcuException.Kind.Protocol, EcuException.Kind.Exchange -> OperationFailure.Exchange
        }
        error is IOException -> OperationFailure.Exchange
        else -> OperationFailure.Unexpected
    }
}
