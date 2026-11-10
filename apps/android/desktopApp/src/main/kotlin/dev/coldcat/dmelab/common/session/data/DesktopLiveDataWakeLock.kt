package dev.coldcat.dmelab.common.session.data

import dev.coldcat.dmelab.common.session.domain.LiveDataWakeLock
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/** The assertion ends on normal cleanup and also when the owning JVM exits. */
@Single
internal class DesktopLiveDataWakeLock(
    @Named("io") private val io: CoroutineDispatcher,
) : LiveDataWakeLock {
    override suspend fun hold(action: suspend () -> Unit) {
        var process: Process? = null
        try {
            withContext(context = io) {
                process = ProcessBuilder("/usr/bin/caffeinate", "-i", "-w", ProcessHandle.current().pid().toString())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start()
                    .also { if (!it.isAlive) throw IOException("Cannot prevent system idle sleep") }
            }
            action()
        } finally {
            withContext(context = NonCancellable + io) {
                process?.let { assertion ->
                    assertion.destroy()
                    if (!assertion.waitFor(2, TimeUnit.SECONDS)) assertion.destroyForcibly()
                }
            }
        }
    }
}
