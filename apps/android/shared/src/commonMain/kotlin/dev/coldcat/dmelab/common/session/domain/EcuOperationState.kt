package dev.coldcat.dmelab.common.session.domain

data class EcuOperationState(
    val active: ActiveOperation? = null,
    val resultSessionId: Long? = null,
    val dtcs: DiagnosticResult<List<DtcRecord>> = DiagnosticResult(),
    val liveData: DiagnosticResult<List<LiveValue>> = DiagnosticResult(),
    val liveDataTiming: LiveDataTiming? = null,
    val liveDataRecoveryCount: ULong? = null,
    val dtcClear: DiagnosticResult<Unit> = DiagnosticResult(),
    val flash: FlashResult? = null,
    val failure: OperationFailure? = null,
    val failureKind: OperationKind? = null,
)
