package dev.coldcat.dmelab.common.session.data

import dev.coldcat.dmelab.common.session.domain.OperationJournal
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
internal class DesktopOperationJournal(
    private val storage: DesktopStorage,
    @Named("io") private val io: CoroutineDispatcher,
) : OperationJournal {
    private val journal = File(storage.directory, "operation-running")
    private val mutex = Mutex()
    override val wasRunning: Boolean get() = journal.exists()

    override suspend fun markRunning(running: Boolean): Boolean = withContext(context = io) {
        mutex.withLock {
            try {
                storage.prepare()
                if (!running) {
                    Files.deleteIfExists(journal.toPath())
                } else {
                    val temporary = Files.createTempFile(storage.directory.toPath(), ".operation-", ".part")
                    try {
                        FileOutputStream(temporary.toFile()).use { output ->
                            output.write("running\n".toByteArray(charset = Charsets.UTF_8))
                            output.fd.sync()
                        }
                        try {
                            Files.move(temporary, journal.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                        } catch (_: AtomicMoveNotSupportedException) {
                            Files.move(temporary, journal.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        }
                    } finally {
                        Files.deleteIfExists(temporary)
                    }
                }
                true
            } catch (_: IOException) {
                false
            } catch (_: SecurityException) {
                false
            }
        }
    }
}
