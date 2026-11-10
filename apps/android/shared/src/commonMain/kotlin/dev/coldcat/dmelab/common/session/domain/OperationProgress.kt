package dev.coldcat.dmelab.common.session.domain

data class OperationProgress(val stage: String, val completed: Long, val total: Long)
