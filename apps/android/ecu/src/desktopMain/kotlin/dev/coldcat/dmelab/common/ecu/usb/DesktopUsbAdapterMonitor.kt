package dev.coldcat.dmelab.common.ecu.usb

import dev.coldcat.dmelab.common.ecu.model.UsbAdapter
import dev.coldcat.dmelab.common.ecu.model.UsbInventory
import dev.coldcat.dmelab.common.ecu.session.nativeCall
import dev.coldcat.dmelab.ffi.listSerialPorts
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/** Native enumeration runs on IO; polling observes unplug without opening any port. */
@Single
internal class DesktopUsbAdapterMonitor(
    @Named("application") private val scope: CoroutineScope,
    @Named("io") private val io: CoroutineDispatcher,
) : UsbAdapterMonitor {
    private val mutableInventory = MutableStateFlow<UsbInventory>(UsbInventory.Scanning)
    override val inventory = mutableInventory.asStateFlow()
    private val enumeration = Mutex()

    init {
        scope.launch(context = io) {
            while (true) {
                enumerate()
                delay(timeMillis = REFRESH_INTERVAL_MS)
            }
        }
    }

    override fun refresh() {
        scope.launch(context = io) { enumerate() }
    }

    override fun requestPermission(id: String, force: Boolean) {
        // Desktop ports use OS permissions; opening reports an actionable native error.
        refresh()
    }

    private fun enumerate() {
        if (!enumeration.tryLock()) return
        try {
            val ports = nativeCall { listSerialPorts() }
            val names = ports.mapTo(mutableSetOf()) { it.name }
            val adapters = ports.filterNot { port ->
                // macOS exposes the same physical device through both tty and cu.
                port.name.startsWith("/dev/tty.") &&
                    port.name.replaceFirst(oldValue = "/dev/tty.", newValue = "/dev/cu.") in names
            }.map { port ->
                UsbAdapter(
                    id = port.name,
                    name = port.description?.takeIf(String::isNotBlank)
                        ?.let { description -> "$description (${port.name})" } ?: port.name,
                    vendorId = port.usbVid?.toInt() ?: 0,
                    productId = port.usbPid?.toInt() ?: 0,
                    hasPermission = true,
                )
            }.sortedBy(UsbAdapter::id)
            mutableInventory.value = UsbInventory.Available(
                adapters = adapters,
                unsupportedCount = 0,
                permissionPendingId = null,
                permissionDeniedId = null,
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            mutableInventory.value = UsbInventory.Failed
        } finally {
            enumeration.unlock()
        }
    }

    private companion object {
        const val REFRESH_INTERVAL_MS = 5000L
    }
}
