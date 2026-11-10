package dev.coldcat.dmelab.common.session.data

import dev.coldcat.dmelab.common.session.domain.OperationHost
import dev.coldcat.dmelab.common.session.domain.OperationRuntime
import dev.coldcat.dmelab.common.session.domain.OperationKind
import dev.coldcat.dmelab.common.session.domain.OperationFailure
import dev.coldcat.dmelab.common.session.domain.LiveDataWakeLock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/** Bound at the composition root before UI starts, avoiding a host/runtime DI cycle. */
@Single
internal class DesktopOperationHost(
    @Named("application") private val scope: CoroutineScope,
    private val wakeLock: LiveDataWakeLock,
) : OperationHost {
    private var runtime: OperationRuntime? = null

    fun attach(runtime: OperationRuntime) {
        check(this.runtime == null)
        this.runtime = runtime
    }

    override fun start() {
        val current = checkNotNull(runtime)
        scope.launch {
            // Match a service start: admission must publish the polling configuration first.
            yield()
            try {
                val kind = current.state.value.active?.kind
                if (kind == OperationKind.ReadFlash || kind == OperationKind.WriteFlash) {
                    wakeLock.hold { current.runPending(scope = scope)?.join() }
                } else current.runPending(scope = scope)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                current.rejectPending(problem = OperationFailure.Unexpected)
            }
        }
    }
}
