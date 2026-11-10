package dev.coldcat.dmelab.common.session.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Job

/** Narrow service entry point; the same singleton also implements EcuOperations. */
interface OperationRuntime {
    val state: StateFlow<EcuOperationState>
    fun rejectPending(problem: OperationFailure)
    fun runPending(scope: CoroutineScope): Job?
    fun stopLiveData()
}
