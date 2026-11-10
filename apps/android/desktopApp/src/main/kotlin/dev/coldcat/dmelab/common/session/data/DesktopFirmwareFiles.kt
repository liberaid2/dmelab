package dev.coldcat.dmelab.common.session.data

import dev.coldcat.dmelab.common.ecu.model.FirmwareMode
import dev.coldcat.dmelab.common.ecu.session.FirmwareValidator
import dev.coldcat.dmelab.common.session.domain.FirmwareFileException
import dev.coldcat.dmelab.common.session.domain.FirmwareFiles
import dev.coldcat.dmelab.common.session.domain.FirmwareImage
import dev.coldcat.dmelab.common.session.domain.OperationFailure
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
internal class DesktopFirmwareFiles(
    private val storage: DesktopStorage,
    private val documents: DesktopDocuments,
    private val validator: FirmwareValidator,
    @Named("io") private val io: CoroutineDispatcher,
) : FirmwareFiles {
    override suspend fun importImage(source: String): FirmwareImage = withContext(context = io) {
        val name = imageFile(source = source).name
        val bytes = read(source = source)
        val mode = when (bytes.size) {
            CALIBRATION_SIZE -> FirmwareMode.Calibration
            MAX_IMAGE_SIZE -> FirmwareMode.Full
            else -> throw FirmwareFileException(problem = OperationFailure.InvalidImage)
        }
        validate(bytes = bytes, mode = mode)
        val directory = File(storage.cacheDirectory, "firmware_imports")
        val copyDirectory = File(directory, UUID.randomUUID().toString())
        try {
            if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot create import directory")
            directory.listFiles()?.filter { it.isDirectory && it.lastModified() < System.currentTimeMillis() - IMPORT_RETENTION_MS }
                ?.forEach { it.deleteRecursively() }
            if (!copyDirectory.mkdir()) throw IOException("Cannot create import")
            val copy = File(copyDirectory, name)
            copy.outputStream().use { output -> output.write(bytes) }
            currentCoroutineContext().ensureActive()
            FirmwareImage(location = copy.toURI().toString(), name = name, byteCount = bytes.size,
                mode = mode, fingerprint = fingerprint(bytes = bytes))
        } catch (exception: CancellationException) {
            copyDirectory.deleteRecursively()
            throw exception
        } catch (_: Exception) {
            copyDirectory.deleteRecursively()
            throw FirmwareFileException(problem = OperationFailure.FileAccess)
        }
    }

    override suspend fun inspect(source: String, mode: FirmwareMode): FirmwareImage = withContext(context = io) {
        val name = imageFile(source = source).name
        val bytes = read(source = source)
        validate(bytes = bytes, mode = mode)
        FirmwareImage(location = source, name = name, byteCount = bytes.size, mode = mode,
            fingerprint = fingerprint(bytes = bytes))
    }

    override suspend fun read(source: String): ByteArray = withContext(context = io) {
        try {
            documents.file(location = source).inputStream().use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (output.size() <= MAX_IMAGE_SIZE) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(buffer, 0, minOf(buffer.size, MAX_IMAGE_SIZE + 1 - output.size()))
                    if (count < 0) break
                    if (count > 0) output.write(buffer, 0, count)
                }
                if (output.size() > MAX_IMAGE_SIZE) throw FirmwareFileException(problem = OperationFailure.InvalidImage)
                output.toByteArray()
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: FirmwareFileException) {
            throw exception
        } catch (_: Exception) {
            throw FirmwareFileException(problem = OperationFailure.FileAccess)
        }
    }

    override fun fingerprint(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(separator = "") { "%02x".format(it) }

    override suspend fun save(destination: String, readImage: suspend () -> ByteArray) {
        withContext(context = io) {
            try {
                documents.write(destination = destination) { output ->
                    val bytes = readImage()
                    currentCoroutineContext().ensureActive()
                    output.write(bytes)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: IOException) {
                throw FirmwareFileException(problem = OperationFailure.FileAccess)
            } catch (_: SecurityException) {
                throw FirmwareFileException(problem = OperationFailure.FileAccess)
            } catch (_: IllegalArgumentException) {
                throw FirmwareFileException(problem = OperationFailure.FileAccess)
            }
        }
    }

    private fun imageFile(source: String): File {
        val file = try {
            documents.file(location = source)
        } catch (_: Exception) {
            throw FirmwareFileException(problem = OperationFailure.FileAccess)
        }
        if (!file.name.endsWith(suffix = ".bin", ignoreCase = true)) {
            throw FirmwareFileException(problem = OperationFailure.InvalidImage)
        }
        return file
    }

    private suspend fun validate(bytes: ByteArray, mode: FirmwareMode) {
        try {
            validator.validateImage(image = bytes, mode = mode)
        } catch (exception: CancellationException) {
            throw exception
        } catch (error: Exception) {
            throw FirmwareFileException(problem = OperationErrors.from(error = error))
        } catch (_: LinkageError) {
            throw FirmwareFileException(problem = OperationFailure.NativeLibrary)
        }
    }

    private companion object {
        const val MAX_IMAGE_SIZE = 512 * 1024
        const val CALIBRATION_SIZE = 64 * 1024
        const val IMPORT_RETENTION_MS = 24 * 60 * 60 * 1000L
    }
}
