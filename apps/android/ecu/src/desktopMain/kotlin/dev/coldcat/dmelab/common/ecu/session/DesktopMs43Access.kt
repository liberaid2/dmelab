package dev.coldcat.dmelab.common.ecu.session

import dev.coldcat.dmelab.common.ecu.EcuException
import dev.coldcat.dmelab.ffi.Ms43Client
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/** One owner per operation; the native client owns and closes its serial handle. */
@Single
internal class DesktopMs43Access(
    @Named("ecu") private val dispatcher: CoroutineDispatcher,
) : Ms43Access {
    private val busy = AtomicBoolean(false)

    override suspend fun <T> use(adapterId: String, operation: suspend (EcuClient) -> T): T =
        useNative(adapterId = adapterId, operation = operation)

    internal suspend fun <T> useNative(adapterId: String, operation: suspend (DesktopEcuClient) -> T): T {
        if (!busy.compareAndSet(false, true)) {
            throw EcuException(kind = EcuException.Kind.Busy, cause = IllegalStateException("ECU session already in use"))
        }
        try {
            return nativeCall {
                withContext(context = dispatcher) {
                    Ms43Client.openSerial(portName = adapterId, timeoutMs = OPEN_TIMEOUT_MS).use { client ->
                        operation(DesktopEcuClient(native = client))
                    }
                }
            }
        } finally {
            busy.set(false)
        }
    }

    private companion object {
        const val OPEN_TIMEOUT_MS = 1500u
    }
}
