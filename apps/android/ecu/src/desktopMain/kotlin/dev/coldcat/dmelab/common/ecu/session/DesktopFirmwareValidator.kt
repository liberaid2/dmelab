package dev.coldcat.dmelab.common.ecu.session

import dev.coldcat.dmelab.common.ecu.model.FirmwareMode
import dev.coldcat.dmelab.ffi.FlashMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/** Pure Rust validation on IO, independently of the session executor. */
@Single
internal class DesktopFirmwareValidator(
    @Named("io") private val io: CoroutineDispatcher,
) : FirmwareValidator {
    override suspend fun validateImage(image: ByteArray, mode: FirmwareMode): Unit = withContext(context = io) {
        nativeCall { dev.coldcat.dmelab.ffi.validateImage(image = image, mode = mode.toNative()) }
    }
}

internal fun FirmwareMode.toNative(): FlashMode = when (this) {
    FirmwareMode.Full -> FlashMode.FULL
    FirmwareMode.Calibration -> FlashMode.CALIBRATION
}
