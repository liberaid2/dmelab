package dev.coldcat.dmelab.common.session.data

import android.content.Context
import android.os.SystemClock
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import dev.coldcat.dmelab.common.ecu.model.LiveDataSelection
import dev.coldcat.dmelab.common.ecu.model.SelectedLiveData
import dev.coldcat.dmelab.common.session.domain.LiveDataLog
import dev.coldcat.dmelab.common.session.domain.LiveDataLogArchive
import dev.coldcat.dmelab.common.session.domain.LiveDataLogName
import dev.coldcat.dmelab.common.session.domain.LiveDataLogs
import java.io.BufferedWriter
import java.io.File
import java.io.InputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/** Service-owned writer, persistent app-private files, and bounded streaming IO. */
@Single
internal class AndroidLiveDataLogs(
    @Provided private val context: Context,
    private val archives: AndroidLiveDataArchives,
    @Named("io") private val io: CoroutineDispatcher,
) : LiveDataLogs {
    private val directory = File(context.filesDir, "live_data_logs")
    private val mutex = Mutex()
    private val mutableLogs = MutableStateFlow<List<LiveDataLog>>(emptyList())
    override val logs = mutableLogs.asStateFlow()
    private var loaded = false
    private var writer: BufferedWriter? = null
    private var csv: LiveDataCsv? = null
    private var active: LiveDataLog? = null
    private var startedAt = 0L

    override suspend fun load() = withContext(context = io) { mutex.withLock { loadLocked() } }

    override suspend fun start(selection: LiveDataSelection) = withContext(context = io) {
        mutex.withLock {
            loadLocked()
            check(writer == null)
            val format = LiveDataCsv(selection = selection)
            val stamp = DateTimeFormatter.ofPattern("uuuu-MM-dd_HH-mm-ss", Locale.ROOT)
                .withZone(ZoneId.systemDefault()).format(Instant.now())
            val id = availableLogId(stem = "ms43_live_$stamp")
            val file = File(directory, "$id.part")
            if (!file.createNewFile()) throw IOException("Cannot reserve CSV")
            val output = file.bufferedWriter(charset = Charsets.UTF_8)
            try {
                output.write(format.header + "\r\n")
                output.flush()
            } catch (exception: IOException) {
                try { output.close() } catch (_: IOException) { /* Preserve original failure. */ }
                throw exception
            }
            writer = output
            csv = format
            startedAt = SystemClock.elapsedRealtime()
            active = LiveDataLog(id = id, samples = 0, byteCount = file.length(), elapsedMillis = 0, recording = true)
            publishActive()
        }
    }

    override suspend fun append(data: SelectedLiveData, timestampMillis: Long, completedAtMillis: Long) = withContext(context = io) {
        mutex.withLock {
            val current = checkNotNull(active)
            val elapsed = (completedAtMillis - startedAt).coerceAtLeast(minimumValue = 0)
            val output = checkNotNull(writer)
            output.write(checkNotNull(csv).row(data = data, timestampMillis = timestampMillis, elapsedMillis = elapsed) + "\r\n")
            // Flush each complete set; no in-memory history or unbounded queue of samples.
            output.flush()
            active = current.copy(samples = current.samples + 1, elapsedMillis = elapsed,
                byteCount = File(directory, "${current.id}.part").length())
            publishActive()
        }
    }

    override suspend fun finish(partial: Boolean): Unit = withContext(context = io) {
        mutex.withLock {
            val current = active ?: return@withLock
            var closeFailure: IOException? = null
            try { writer?.close() } catch (exception: IOException) { closeFailure = exception }
            writer = null
            csv = null
            active = null
            try {
                val source = File(directory, "${current.id}.part")
                // A failed flush may leave bytes from an uncommitted row. Keep only
                // the prefix whose flush succeeded, even if close retried buffered IO.
                RandomAccessFile(source, "rw").use { it.setLength(current.byteCount) }
                val id = availableLogId(stem = current.id.removeSuffix(".csv"),
                    partial = partial || closeFailure != null, source = source)
                val destination = File(directory, id)
                if (!source.renameTo(destination)) throw IOException("Cannot finalize CSV")
                val result = if (closeFailure == null) current.copy(id = id, byteCount = destination.length(),
                    recording = false, partial = partial) else inspect(file = destination)
                mutableLogs.value = listOf(result) + mutableLogs.value.filterNot { it.id == current.id }
            } catch (exception: IOException) {
                // Keep the failed entry visible; a subsequent load retries recovery.
                loaded = false
                mutableLogs.value = listOf(current.copy(recording = false, partial = true, available = false)) +
                    mutableLogs.value.filterNot { it.id == current.id }
                throw exception
            }
            closeFailure?.let { throw it }
            Unit
        }
    }

    override suspend fun shareUri(id: String): String = withContext(context = io) {
        mutex.withLock {
            val file = completedFile(id = id)
            sharedUri(file = file)
        }
    }

    override suspend fun export(id: String, destination: String) = withContext(context = io) {
        // Open under the lock, then copy without delaying the active sample writer.
        val source = mutex.withLock { completedFile(id = id).inputStream() }
        source.use { input -> copyToDestination(input = input, destination = destination) }
    }

    override suspend fun shareUris(ids: List<String>): List<String> = withContext(context = io) {
        mutex.withLock {
            require(ids.isNotEmpty())
            val files = ids.distinct().map { id -> completedFile(id = id) }
            // Resolve the whole selection before publishing any URI; never silently omit a file.
            files.map { file -> sharedUri(file = file) }
        }
    }

    override suspend fun createArchive(): LiveDataLogArchive {
        val sources = withContext(context = io) {
            mutex.withLock {
                loadLocked()
                mutableLogs.value.filter { !it.recording && it.available }.map { completedFile(id = it.id) }
            }
        }
        // These CSVs are finalized and immutable. If a source disappears, fail the
        // entire archive instead of silently exporting an incomplete selection.
        return archives.create(sources = sources)
    }

    override suspend fun shareArchiveUri(id: String): String = withContext(context = io) {
        sharedUri(file = archives.file(id = id))
    }

    override suspend fun exportArchive(id: String, destination: String) = withContext(context = io) {
        archives.file(id = id).inputStream().use { input -> copyToDestination(input = input, destination = destination) }
    }

    private fun sharedUri(file: File): String =
        FileProvider.getUriForFile(context, "${context.packageName}.live-data-logs", file).toString()

    private suspend fun copyToDestination(input: InputStream, destination: String) {
        val uri = destination.toUri()
        require(uri.scheme == "content")
        val output = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot open destination")
        val coroutine = currentCoroutineContext()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        output.use { target ->
            while (true) {
                coroutine.ensureActive()
                val count = input.read(buffer)
                if (count < 0) break
                target.write(buffer, 0, count)
            }
        }
    }

    override suspend fun delete(id: String) = withContext(context = io) {
        mutex.withLock {
            if (!completedFile(id = id).delete()) throw IOException("Cannot delete CSV")
            mutableLogs.value = mutableLogs.value.filterNot { it.id == id }
        }
    }

    override suspend fun rename(id: String, name: String): LiveDataLog = withContext(context = io) {
        mutex.withLock {
            val current = mutableLogs.value.firstOrNull { it.id == id && !it.recording && it.available }
                ?: throw IOException("Log is not available for renaming")
            val stem = requireNotNull(LiveDataLogName.stem(input = name, partial = current.partial))
            val requested = if (current.partial) "${stem}_partial.csv" else "$stem.csv"
            if (requested == id) return@withLock current
            val source = completedFile(id = id)
            val newId = availableLogId(stem = stem, partial = current.partial, source = source)
            if (!source.renameTo(File(directory, newId))) throw IOException("Cannot rename CSV")
            val renamed = current.copy(id = newId)
            // Rename keeps file contents, modification time and history order intact.
            mutableLogs.value = mutableLogs.value.map { if (it.id == id) renamed else it }
            renamed
        }
    }

    private fun loadLocked() {
        if (loaded) return
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot create log directory")
        val files = directory.listFiles() ?: throw IOException("Cannot list logs")
        // A leftover .part belongs to a dead process. Never resume it or expose a torn row.
        files.filter { it.name.endsWith(".csv.part") }.forEach { file ->
            trimIncompleteRow(file = file)
            if (file.length() == 0L) {
                if (!file.delete()) throw IOException("Cannot discard empty log")
                return@forEach
            }
            val destination = File(directory, availableLogId(stem = file.name.removeSuffix(".csv.part"),
                partial = true, source = file))
            if (!file.renameTo(destination)) throw IOException("Cannot recover CSV")
        }
        mutableLogs.value = (directory.listFiles() ?: throw IOException("Cannot list logs"))
            // Modification times order both legacy UTC names and local names across zone changes.
            .filter { it.name.endsWith(".csv") }.sortedByDescending { it.lastModified() }.map { inspect(file = it) }
        loaded = true
    }

    private fun completedFile(id: String): File {
        require(id == File(id).name && id.endsWith(".csv"))
        require(active?.id != id)
        return File(directory, id).also { if (!it.isFile) throw IOException("Log no longer exists") }
    }

    /** Caller holds the writer mutex; reserve normal, active and partial forms of each name. */
    private fun availableLogId(stem: String, partial: Boolean = false, source: File? = null): String {
        var number = 0L
        while (true) {
            val candidate = if (number == 0L) stem else "$stem-$number"
            val occupied = listOf("$candidate.csv", "$candidate.csv.part", "${candidate}_partial.csv")
                .map { File(directory, it) }.any { it != source && it.exists() }
            if (!occupied) return if (partial) "${candidate}_partial.csv" else "$candidate.csv"
            number++
        }
    }

    private fun publishActive() {
        val current = checkNotNull(active)
        mutableLogs.value = listOf(current) + mutableLogs.value.filterNot { it.id == current.id }
    }

    private fun inspect(file: File): LiveDataLog {
        var count = 0L
        var elapsed = 0L
        file.bufferedReader(charset = Charsets.UTF_8).use { input ->
            input.readLine() // Header, not a sample.
            input.lineSequence().forEach { line ->
                if (line.isNotEmpty()) {
                    count++
                    elapsed = line.substringAfter(',').substringBefore(',').toLongOrNull() ?: elapsed
                }
            }
        }
        return LiveDataLog(id = file.name, samples = count, byteCount = file.length(), elapsedMillis = elapsed,
            partial = file.name.endsWith("_partial.csv"))
    }

    private fun trimIncompleteRow(file: File) {
        RandomAccessFile(file, "rw").use { output ->
            var end = output.length()
            while (end > 0) {
                output.seek(end - 1)
                if (output.readByte() == '\n'.code.toByte()) break
                end--
            }
            output.setLength(end)
        }
    }
}
