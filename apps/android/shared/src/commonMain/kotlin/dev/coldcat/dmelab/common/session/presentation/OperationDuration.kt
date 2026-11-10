package dev.coldcat.dmelab.common.session.presentation

/** A duration, not a wall-clock time; hours are included only when needed. */
internal fun formatOperationDuration(elapsedMillis: Long): String {
    val seconds = elapsedMillis.coerceAtLeast(minimumValue = 0L) / 1000
    val hours = seconds / 3600
    val parts = if (hours > 0) listOf(hours, seconds / 60 % 60, seconds % 60)
        else listOf(seconds / 60, seconds % 60)
    return parts.joinToString(separator = ":") { it.toString().padStart(length = 2, padChar = '0') }
}
