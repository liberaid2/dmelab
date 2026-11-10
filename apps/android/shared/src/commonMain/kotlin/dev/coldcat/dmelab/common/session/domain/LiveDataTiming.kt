package dev.coldcat.dmelab.common.session.domain

data class LiveDataTiming(
    val readDurationMillis: Long? = null,
    val sampleIntervalMillis: Long? = null,
    val updateFrequencyHz: Double? = null,
)
