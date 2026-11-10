package dev.coldcat.dmelab.common.session.domain

data class LiveDataLog(
    val id: String,
    val samples: Long,
    val byteCount: Long,
    val elapsedMillis: Long,
    val recording: Boolean = false,
    val partial: Boolean = false,
    val available: Boolean = true,
)
