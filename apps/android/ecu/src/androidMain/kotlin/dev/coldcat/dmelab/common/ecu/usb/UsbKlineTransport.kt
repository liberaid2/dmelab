package dev.coldcat.dmelab.common.ecu.usb

import com.hoho.android.usbserial.driver.SerialTimeoutException
import com.hoho.android.usbserial.driver.UsbSerialPort
import dev.coldcat.dmelab.ffi.KlineTransport
import dev.coldcat.dmelab.ffi.TransportFailure
import java.io.IOException

/** Synchronous transport owned by one ECU operation. No reader thread competes with Rust. */
internal class UsbKlineTransport(
    private val port: UsbSerialPort,
    private val isAttached: () -> Boolean,
) : KlineTransport {
    private var buffered = byteArrayOf()

    override fun configureInitial() = transportCall {
        port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_EVEN)
        port.flowControl = UsbSerialPort.FlowControl.NONE
        port.rts = false
        port.dtr = false
    }

    override fun read(maxBytes: UInt, timeoutMs: UInt): ByteArray = transportCall {
        require(maxBytes in 1u..1_048_576u)
        require(timeoutMs in 1u..60_000u)
        if (buffered.isEmpty()) {
            // USB drivers may consume a complete packet even for a short protocol read.
            val incoming = ByteArray(maxOf(maxBytes.toInt(), USB_READ_BUFFER_SIZE))
            val count = port.read(incoming, timeoutMs.toInt())
            buffered = incoming.copyOf(count)
        }
        val count = minOf(maxBytes.toInt(), buffered.size)
        val result = buffered.copyOfRange(0, count)
        buffered = buffered.copyOfRange(count, buffered.size)
        result
    }

    override fun write(data: ByteArray, timeoutMs: UInt): UInt = transportCall {
        require(timeoutMs in 1u..60_000u)
        try {
            port.write(data, timeoutMs.toInt())
            data.size.toUInt()
        } catch (exception: SerialTimeoutException) {
            if (exception.bytesTransferred <= 0) {
                throw TransportFailure.Timeout(details = "USB write timed out")
            }
            exception.bytesTransferred.coerceAtMost(data.size).toUInt()
        }
    }

    override fun setBaudRate(baudRate: UInt) = transportCall {
        require(baudRate in 1u..Int.MAX_VALUE.toUInt())
        port.setParameters(baudRate.toInt(), 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_EVEN)
    }

    override fun clearBuffers() = transportCall {
        buffered = byteArrayOf()
        port.purgeHwBuffers(true, true)
        Unit
    }

    private fun <T> transportCall(block: () -> T): T {
        if (!isAttached()) throw TransportFailure.Disconnected(details = "USB adapter detached")
        try {
            return block()
        } catch (exception: SerialTimeoutException) {
            throw TransportFailure.Timeout(details = "USB operation timed out")
        } catch (exception: IOException) {
            if (!isAttached()) throw TransportFailure.Disconnected(details = "USB adapter detached")
            throw TransportFailure.Other(details = exception.message ?: "USB I/O failed")
        } catch (exception: IllegalArgumentException) {
            throw TransportFailure.Other(details = exception.message ?: "Unsupported USB parameters")
        } catch (exception: UnsupportedOperationException) {
            throw TransportFailure.Other(details = exception.message ?: "Unsupported USB operation")
        }
    }

    private companion object {
        const val USB_READ_BUFFER_SIZE = 4096
    }
}
