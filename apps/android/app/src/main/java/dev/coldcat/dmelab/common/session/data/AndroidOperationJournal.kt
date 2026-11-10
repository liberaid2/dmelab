package dev.coldcat.dmelab.common.session.data

import android.content.Context
import dev.coldcat.dmelab.common.session.domain.OperationJournal
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

@Single
internal class AndroidOperationJournal(
    @Provided context: Context,
    @Named("io") private val io: CoroutineDispatcher,
) : OperationJournal {
    private val journal = context.getSharedPreferences("ecu_operation", Context.MODE_PRIVATE)
    override val wasRunning: Boolean get() = journal.getBoolean("running", false)

    override suspend fun markRunning(running: Boolean): Boolean = withContext(context = io) {
        journal.edit().putBoolean("running", running).commit()
    }
}
