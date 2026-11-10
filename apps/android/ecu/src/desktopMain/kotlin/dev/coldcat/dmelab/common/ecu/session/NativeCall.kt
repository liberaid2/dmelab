package dev.coldcat.dmelab.common.ecu.session

import dev.coldcat.dmelab.common.ecu.EcuException
import dev.coldcat.dmelab.ffi.DmeException

internal inline fun <T> nativeCall(action: () -> T): T = try {
    action()
} catch (error: DmeException) {
    val kind = when (error) {
        is DmeException.Busy -> EcuException.Kind.Busy
        is DmeException.NeedsReconnect, is DmeException.OperationAndRestore -> EcuException.Kind.NeedsReconnect
        is DmeException.InvalidImage -> EcuException.Kind.InvalidImage
        is DmeException.LowVoltage -> EcuException.Kind.LowVoltage
        is DmeException.FirmwareMismatch -> EcuException.Kind.FirmwareMismatch
        is DmeException.Protocol -> EcuException.Kind.Protocol
        is DmeException.Transport, is DmeException.InvalidArgument, is DmeException.DtcClear,
        is DmeException.LiveDataRead, is DmeException.DiagnosticControl, is DmeException.DtcRead,
        is DmeException.Flash -> EcuException.Kind.Exchange
    }
    throw EcuException(kind = kind, cause = error)
} catch (error: SecurityException) {
    throw EcuException(kind = EcuException.Kind.Permission, cause = error)
} catch (error: LinkageError) {
    throw EcuException(kind = EcuException.Kind.NativeLibrary, cause = error)
}
