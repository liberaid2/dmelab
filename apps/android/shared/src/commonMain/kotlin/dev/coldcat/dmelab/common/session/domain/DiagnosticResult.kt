package dev.coldcat.dmelab.common.session.domain

data class DiagnosticResult<T>(val value: T? = null, val updatedAt: Long? = null, val failure: OperationFailure? = null)
