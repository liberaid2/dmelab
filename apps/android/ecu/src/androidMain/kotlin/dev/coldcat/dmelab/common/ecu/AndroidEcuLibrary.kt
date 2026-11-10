package dev.coldcat.dmelab.common.ecu

import dev.coldcat.dmelab.ffi.libraryVersion
import dev.coldcat.dmelab.common.ecu.session.nativeCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/** Main-safe access to native library metadata; session operations need their own owner. */
@Single
internal class AndroidEcuLibrary(
    @Named("io") private val ioDispatcher: CoroutineDispatcher,
) : EcuLibrary {
    override suspend fun version(): String = withContext(context = ioDispatcher) {
        nativeCall { libraryVersion() }
    }
}
