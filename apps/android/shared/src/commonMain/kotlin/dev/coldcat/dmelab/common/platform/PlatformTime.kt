package dev.coldcat.dmelab.common.platform

/** Monotonic elapsed time; Android includes deep sleep to preserve operation durations. */
internal expect fun monotonicTimeNanos(): Long
