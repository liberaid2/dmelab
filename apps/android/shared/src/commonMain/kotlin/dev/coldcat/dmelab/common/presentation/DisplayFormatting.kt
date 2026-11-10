package dev.coldcat.dmelab.common.presentation

import kotlin.math.abs
import kotlin.math.round
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

internal fun formatLocalTime(epochMillis: Long, includeYear: Boolean = false): String {
    val date = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    val day = date.day.toString().padStart(2, '0')
    val month = date.month.number.toString().padStart(2, '0')
    val hour = date.hour.toString().padStart(2, '0')
    val minute = date.minute.toString().padStart(2, '0')
    val second = date.second.toString().padStart(2, '0')
    val year = if (includeYear) ".${date.year}" else ""
    return "$day.$month$year $hour:$minute:$second"
}

internal fun formatDecimal(value: Double, decimals: Int = 2, fixed: Boolean = false): String {
    if (!value.isFinite()) return value.toString()
    require(decimals in 0..6)
    var factor = 1L
    repeat(decimals) { factor *= 10 }
    val scaled = round(value * factor).toLong()
    val magnitude = abs(scaled)
    val sign = if (scaled < 0) "-" else ""
    val whole = magnitude / factor
    if (decimals == 0) return "$sign$whole"
    val fraction = (magnitude % factor).toString().padStart(decimals, '0')
        .let { if (fixed) it else it.trimEnd('0') }
    return if (fraction.isEmpty()) "$sign$whole" else "$sign$whole.$fraction"
}
