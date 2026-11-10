package dev.coldcat.dmelab.common.ecu.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.hoho.android.usbserial.driver.FtdiSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialProber
import dev.coldcat.dmelab.common.ecu.model.UsbAdapter
import dev.coldcat.dmelab.common.ecu.model.UsbInventory
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/** Application-lifetime USB observer. Broadcasts trigger enumeration; extras are not trusted. */
@Single
internal class AndroidUsbAdapterMonitor(@Provided private val context: Context) : UsbAdapterMonitor {
    internal val manager: UsbManager = context.getSystemService(UsbManager::class.java)
    private val mutableInventory = MutableStateFlow<UsbInventory>(UsbInventory.Scanning)
    override val inventory = mutableInventory.asStateFlow()
    private val attemptedPermissions = mutableSetOf<String>()
    private var pendingId: String? = null
    private var deniedId: String? = null
    private val permissionAction = "${context.packageName}.USB_PERMISSION"
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == permissionAction) {
                deniedId = pendingId?.takeUnless { id ->
                    manager.deviceList[id]?.let(manager::hasPermission) == true
                }
                pendingId = null
            }
            refresh()
        }
    }

    init {
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
                addAction(permissionAction)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        refresh()
    }

    override fun refresh() {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)) {
            mutableInventory.value = UsbInventory.Unavailable
            return
        }
        try {
            val devices = manager.deviceList
            val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
                .filterIsInstance<FtdiSerialDriver>()
            val adapters = drivers.map { driver ->
                val device = driver.device
                UsbAdapter(
                    id = device.deviceName,
                    name = device.productName?.takeIf(String::isNotBlank) ?: "FTDI K-line",
                    vendorId = device.vendorId,
                    productId = device.productId,
                    hasPermission = manager.hasPermission(device),
                )
            }.sortedBy(UsbAdapter::id)
            attemptedPermissions.retainAll(devices.keys)
            pendingId = pendingId?.takeIf(devices::containsKey)
            deniedId = deniedId?.takeIf(devices::containsKey)
            mutableInventory.value = UsbInventory.Available(
                adapters = adapters,
                unsupportedCount = devices.size - adapters.size,
                permissionPendingId = pendingId,
                permissionDeniedId = deniedId,
            )
        } catch (exception: RuntimeException) {
            Log.e("DmeLabUsb", "USB enumeration failed", exception)
            mutableInventory.value = UsbInventory.Failed
        }
    }

    override fun requestPermission(id: String, force: Boolean) {
        val device = manager.deviceList[id] ?: return refresh()
        if (manager.hasPermission(device)) return refresh()
        if (pendingId != null || (!force && id in attemptedPermissions)) return
        attemptedPermissions += id
        pendingId = id
        deniedId = null
        refresh()
        try {
            // The receiver rechecks UsbManager, so mutable result extras are unnecessary.
            val intent = PendingIntent.getBroadcast(
                context,
                device.deviceId,
                Intent(permissionAction).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            manager.requestPermission(device, intent)
        } catch (exception: RuntimeException) {
            Log.e("DmeLabUsb", "USB permission request failed", exception)
            pendingId = null
            deniedId = id
            refresh()
        }
    }

    internal fun findDriver(id: String): UsbSerialDriver? =
        UsbSerialProber.getDefaultProber().findAllDrivers(manager)
            .filterIsInstance<FtdiSerialDriver>()
            .firstOrNull { it.device.deviceName == id }
}
