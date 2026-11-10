package dev.coldcat.dmelab.common.session.domain

import dev.coldcat.dmelab.common.ecu.model.LiveDataSelection

data class ActiveOperation(
    val kind: OperationKind,
    val progress: OperationProgress? = null,
    val stopping: Boolean = false,
    val polling: Boolean = false,
    val liveDataSelection: LiveDataSelection? = null,
    val elapsedMillis: Long = 0,
)
