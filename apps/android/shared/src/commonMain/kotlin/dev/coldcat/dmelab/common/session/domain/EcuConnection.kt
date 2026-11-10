package dev.coldcat.dmelab.common.session.domain

import kotlinx.coroutines.flow.StateFlow

internal interface EcuConnection {
    val state: StateFlow<EcuConnectionState>
    fun refresh()
    fun selectAdapter(id: String)
    fun requestPermission()
    fun identify()
    fun clearIdentification()
    fun tryReserve(): EcuSessionLease?
    fun release(lease: EcuSessionLease)
    fun invalidate(problem: ConnectionProblem)
}
