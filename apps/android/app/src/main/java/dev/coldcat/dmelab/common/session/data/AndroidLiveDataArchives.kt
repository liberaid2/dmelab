package dev.coldcat.dmelab.common.session.data

import android.content.Context
import dev.coldcat.dmelab.common.session.domain.LiveDataLogArchive
import java.io.File
import java.io.IOException
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/** Independent IO lock: compression never holds the live sample writer's mutex. */
@Single
internal class AndroidLiveDataArchives(
    @Provided context: Context,
    @Named("io") private val io: CoroutineDispatcher,
) {
    private val directory = File(context.cacheDir, "live_data_exports")
    private val mutex = Mutex()

    suspend fun create(sources: List<File>): LiveDataLogArchive = withContext(context = io) {
        mutex.withLock {
            if (sources.isEmpty()) throw IOException("No completed logs to archive")
            prepareDirectory()
            val stamp = DateTimeFormatter.ofPattern("uuuu-MM-dd_HH-mm-ss", Locale.ROOT)
                .withZone(ZoneId.systemDefault()).format(Instant.now())
            val id = availableArchiveId(stem = "ms43_logs_$stamp")
            val temporary = File(directory, "$id.part")
            if (!temporary.createNewFile()) throw IOException("Cannot reserve ZIP")
            var completed = false
            try {
                val coroutine = currentCoroutineContext()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                ZipOutputStream(temporary.outputStream().buffered()).use { zip ->
                    for (source in sources) {
                        coroutine.ensureActive()
                        // CSVs are app-owned, validated basenames; preserve names and contents.
                        zip.putNextEntry(ZipEntry(source.name))
                        source.inputStream().use { input ->
                            while (true) {
                                coroutine.ensureActive()
                                val count = input.read(buffer)
                                if (count < 0) break
                                zip.write(buffer, 0, count)
                            }
                        }
                        zip.closeEntry()
                    }
                }
                coroutine.ensureActive()
                if (!temporary.renameTo(File(directory, id))) throw IOException("Cannot finalize ZIP")
                completed = true
                LiveDataLogArchive(id = id, logCount = sources.size)
            } finally {
                // Never expose a partial ZIP. Failure to remove it is retried by cache cleanup.
                if (!completed) temporary.delete()
            }
        }
    }

    // Called on the repository's IO dispatcher; no ready archive is modified in place.
    fun file(id: String): File {
        require(id == File(id).name && id.endsWith(".zip"))
        return File(directory, id).also { if (!it.isFile) throw IOException("Archive no longer exists") }
    }

    private fun prepareDirectory() {
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot create export directory")
        val cutoff = System.currentTimeMillis() - CACHE_RETENTION_MILLIS
        (directory.listFiles() ?: throw IOException("Cannot list export cache"))
            .filter { it.isFile && it.lastModified() < cutoff && (it.name.endsWith(".zip") || it.name.endsWith(".zip.part")) }
            .forEach { if (!it.delete()) throw IOException("Cannot clear expired export") }
    }

    private fun availableArchiveId(stem: String): String {
        var number = 0L
        while (true) {
            val candidate = if (number == 0L) stem else "$stem-$number"
            val id = "$candidate.zip"
            if (!File(directory, id).exists() && !File(directory, "$id.part").exists()) return id
            number++
        }
    }

    private companion object {
        const val CACHE_RETENTION_MILLIS = 24 * 60 * 60 * 1000L
    }
}
