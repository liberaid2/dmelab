package dev.coldcat.dmelab.common.session.data

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.koin.core.annotation.Single

/** File dialogs authorize destinations; existing files change only after a complete write. */
@Single
internal class DesktopDocuments {
    fun file(location: String): File {
        try {
            val uri = URI(location)
            if (uri.scheme != "file") throw IOException("A local file URI is required")
            return File(uri)
        } catch (error: URISyntaxException) {
            throw IOException("Invalid local file URI", error)
        } catch (error: IllegalArgumentException) {
            throw IOException("Invalid local file URI", error)
        }
    }

    suspend fun write(destination: String, content: suspend (OutputStream) -> Unit) {
        val target = file(location = destination).absoluteFile.toPath()
        val parent = target.parent ?: throw IOException("Missing destination directory")
        if (!Files.isDirectory(parent)) throw IOException("Destination directory is not available")
        if (Files.exists(target) && !Files.isRegularFile(target)) throw IOException("Destination is not a regular file")
        val temporary = Files.createTempFile(parent, ".dme-lab-", ".part")
        var failure: Throwable? = null
        try {
            // Reserve and open before invoking an ECU read, preserving an existing destination on failure.
            FileOutputStream(temporary.toFile()).use { output ->
                content(output)
                output.flush()
                output.fd.sync()
            }
            currentCoroutineContext().ensureActive()
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (error: Throwable) {
            failure = error
            throw error
        } finally {
            try {
                Files.deleteIfExists(temporary)
            } catch (error: Exception) {
                val original = failure
                if (original == null) throw error
                original.addSuppressed(error)
            }
        }
    }
}
