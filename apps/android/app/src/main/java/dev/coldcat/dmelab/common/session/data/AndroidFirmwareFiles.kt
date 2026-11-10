package dev.coldcat.dmelab.common.session.data

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import dev.coldcat.dmelab.common.ecu.model.FirmwareMode
import dev.coldcat.dmelab.common.ecu.session.FirmwareValidator
import dev.coldcat.dmelab.common.session.domain.FirmwareFileException
import dev.coldcat.dmelab.common.session.domain.FirmwareFiles
import dev.coldcat.dmelab.common.session.domain.FirmwareImage
import dev.coldcat.dmelab.common.session.domain.OperationFailure
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

@Single
internal class AndroidFirmwareFiles(
    @Provided private val context: Context,
    private val validator: FirmwareValidator,
    @Named("io") private val io: CoroutineDispatcher,
) : FirmwareFiles {
    override suspend fun importImage(source: String): FirmwareImage = withContext(context = io) {
        if (source.toUri().scheme != "content") throw FirmwareFileException(problem = OperationFailure.FileAccess)
        val name = displayName(source = source)
        if (!name.endsWith(suffix = ".bin", ignoreCase = true)) {
            throw FirmwareFileException(problem = OperationFailure.InvalidImage)
        }
        val bytes = read(source = source)
        val mode = when (bytes.size) {
            CALIBRATION_SIZE -> FirmwareMode.Calibration
            MAX_IMAGE_SIZE -> FirmwareMode.Full
            else -> throw FirmwareFileException(problem = OperationFailure.InvalidImage)
        }
        // Pure image validation needs no ECU, and must not queue behind an active stream.
        validate(bytes = bytes, mode = mode)
        val directory = File(context.cacheDir, "firmware_imports")
        val copyDirectory = File(directory, UUID.randomUUID().toString())
        try {
            if (!directory.isDirectory && !directory.mkdirs()) throw java.io.IOException("Cannot create import directory")
            directory.listFiles()?.filter { it.isDirectory && it.lastModified() < System.currentTimeMillis() - IMPORT_RETENTION_MS }
                ?.forEach { it.deleteRecursively() }
            if (!copyDirectory.mkdir()) throw java.io.IOException("Cannot create import")
            val copy = File(copyDirectory, name)
            copy.outputStream().use { it.write(bytes) }
            coroutineContext.ensureActive()
            val location = FileProvider.getUriForFile(context, "${context.packageName}.live-data-logs", copy).toString()
            FirmwareImage(location = location, name = name, byteCount = bytes.size, mode = mode, fingerprint = fingerprint(bytes = bytes))
        } catch (exception: CancellationException) {
            copyDirectory.deleteRecursively()
            throw exception
        } catch (_: Exception) {
            copyDirectory.deleteRecursively()
            throw FirmwareFileException(problem = OperationFailure.FileAccess)
        }
    }

    override suspend fun inspect(source: String, mode: FirmwareMode): FirmwareImage {
        val bytes = read(source = source)
        validate(bytes = bytes, mode = mode)
        val name = withContext(context = io) { displayName(source = source) }
        return FirmwareImage(location = source, name = name, byteCount = bytes.size, mode = mode, fingerprint = fingerprint(bytes = bytes))
    }

    private suspend fun validate(bytes: ByteArray, mode: FirmwareMode) {
        try {
            withContext(context = io) { validator.validateImage(image = bytes, mode = mode) }
        } catch (exception: CancellationException) {
            throw exception
        } catch (error: Exception) {
            throw FirmwareFileException(problem = OperationErrors.from(error = error))
        } catch (error: LinkageError) {
            throw FirmwareFileException(problem = OperationFailure.NativeLibrary)
        }
    }

    private fun displayName(source: String): String {
        val uri = source.toUri()
        val name = try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: RuntimeException) { null }
        // Never use provider-supplied directory segments as an import destination.
        return (name ?: uri.lastPathSegment ?: "firmware.bin").substringAfterLast('/').substringAfterLast('\\')
    }

    override suspend fun read(source: String): ByteArray = withContext(context = io) {
        try {
            require(source.toUri().scheme == "content")
            context.contentResolver.openInputStream(source.toUri())?.use { stream ->
                // Read at most one byte beyond the largest accepted image; never trust provider metadata.
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (output.size() <= MAX_IMAGE_SIZE) {
                    coroutineContext.ensureActive()
                    val count = stream.read(buffer, 0, minOf(buffer.size, MAX_IMAGE_SIZE + 1 - output.size()))
                    if (count < 0) break
                    if (count == 0) continue
                    output.write(buffer, 0, count)
                }
                if (output.size() > MAX_IMAGE_SIZE) throw FirmwareFileException(problem = OperationFailure.InvalidImage)
                output.toByteArray()
            } ?: throw FirmwareFileException(problem = OperationFailure.FileAccess)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: FirmwareFileException) {
            throw exception
        } catch (exception: Exception) {
            throw FirmwareFileException(problem = OperationFailure.FileAccess)
        }
    }

    override fun fingerprint(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(separator = "") { "%02x".format(it) }

    override suspend fun save(destination: String, readImage: suspend () -> ByteArray) {
        var descriptor: ParcelFileDescriptor? = null
        var saved = false
        try {
            withContext(context = io) {
                try {
                    require(Uri.parse(destination).scheme == "content")
                    descriptor = context.contentResolver.openFileDescriptor(Uri.parse(destination), "wt")
                        ?: throw FirmwareFileException(problem = OperationFailure.FileAccess)
                } catch (exception: Exception) {
                    throw FirmwareFileException(problem = OperationFailure.FileAccess)
                }
            }
            val bytes = readImage()
            withContext(context = io) {
                try {
                    ParcelFileDescriptor.AutoCloseOutputStream(checkNotNull(descriptor)).use { output ->
                        output.write(bytes)
                        output.flush()
                    }
                    descriptor = null
                } catch (exception: Exception) {
                    throw FirmwareFileException(problem = OperationFailure.FileAccess)
                }
            }
            saved = true
        } finally {
            withContext(context = kotlinx.coroutines.NonCancellable + io) {
                runCatching { descriptor?.close() }
                if (!saved) runCatching { DocumentsContract.deleteDocument(context.contentResolver, Uri.parse(destination)) }
            }
        }
    }

    private companion object {
        const val MAX_IMAGE_SIZE = 512 * 1024
        const val CALIBRATION_SIZE = 64 * 1024
        const val IMPORT_RETENTION_MS = 24 * 60 * 60 * 1000L
    }
}
