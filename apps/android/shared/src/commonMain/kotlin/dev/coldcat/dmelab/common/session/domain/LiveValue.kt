package dev.coldcat.dmelab.common.session.domain

data class LiveValue(val label: String, val value: Double?, val unit: String, val flag: Boolean? = null)
