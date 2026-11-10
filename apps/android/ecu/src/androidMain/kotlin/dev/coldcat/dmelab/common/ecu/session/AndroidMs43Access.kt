package dev.coldcat.dmelab.common.ecu.session

import dev.coldcat.dmelab.common.ecu.EcuException
import dev.coldcat.dmelab.common.ecu.usb.AndroidUsbAdapterMonitor
import dev.coldcat.dmelab.common.ecu.usb.UsbKlineTransport
import dev.coldcat.dmelab.ffi.Ms43Client
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/** Operation-scoped port lifetime; callers stay on the ECU executor through cleanup. */
@Single
internal class AndroidMs43Access(
    private val usb: AndroidUsbAdapterMonitor,
    @Named("ecu") private val dispatcher: CoroutineDispatcher,
) : Ms43Access {
    private val busy = AtomicBoolean(false)

    override suspend fun <T> use(adapterId: String, operation: suspend (EcuClient) -> T): T =
        useNative(adapterId = adapterId, operation = operation)

    internal suspend fun <T> useNative(adapterId: String, operation: suspend (AndroidEcuClient) -> T): T {
        if (!busy.compareAndSet(false, true)) {
            throw EcuException(kind = EcuException.Kind.Busy, cause = IllegalStateException("ECU session already in use"))
        }
        try {
            return nativeCall {
                withContext(context = dispatcher) { usePort(adapterId = adapterId, operation = operation) }
            }
        } finally {
            busy.set(false)
        }
    }

    private suspend fun <T> usePort(adapterId: String, operation: suspend (AndroidEcuClient) -> T): T {
        val driver = usb.findDriver(id = adapterId) ?: throw IOException("Adapter detached")
        if (!usb.manager.hasPermission(driver.device)) throw SecurityException("USB permission required")
        val connection = usb.manager.openDevice(driver.device) ?: throw IOException("Cannot open adapter")
        try {
            driver.ports.first().use { port ->
                port.open(connection)
                val transport = UsbKlineTransport(port = port, isAttached = { usb.manager.deviceList.containsKey(adapterId) })
                return Ms43Client(transport = transport).use { client -> nativeCall { operation(AndroidEcuClient(native = client)) } }
            }
        } finally {
            connection.close()
        }
    }
}
