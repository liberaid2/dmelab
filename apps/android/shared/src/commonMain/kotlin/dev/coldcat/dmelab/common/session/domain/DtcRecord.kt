package dev.coldcat.dmelab.common.session.domain

data class DtcRecord(val code: Int, val frequency: Int, val data: String, val raw: String)
