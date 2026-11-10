package dev.coldcat.dmelab.common.ecu.session

import dev.coldcat.dmelab.common.ecu.model.Dtc
import dev.coldcat.dmelab.common.ecu.model.LiveDigital
import dev.coldcat.dmelab.common.ecu.model.LiveMeasurements
import dev.coldcat.dmelab.common.ecu.model.LiveVanos
import dev.coldcat.dmelab.common.ecu.model.Progress
import dev.coldcat.dmelab.common.ecu.model.SelectedLiveData
import dev.coldcat.dmelab.common.ecu.model.VanosPosition

internal fun dev.coldcat.dmelab.ffi.Dtc.toModel(): Dtc = Dtc(
    code = code,
    frequency = frequency,
    data = data,
    raw = raw,
)

internal fun dev.coldcat.dmelab.ffi.LiveMeasurements.toModel(): LiveMeasurements = LiveMeasurements(
    engineSpeedRpm = engineSpeedRpm,
    vehicleSpeedKmh = vehicleSpeedKmh,
    acceleratorPedalAngleDeg = acceleratorPedalAngleDeg,
    throttleAngleDeg = throttleAngleDeg,
    massAirflowKgh = massAirflowKgh,
    intakeAirTemperatureC = intakeAirTemperatureC,
    coolantTemperatureC = coolantTemperatureC,
    oilTemperatureC = oilTemperatureC,
    radiatorOutletTemperatureC = radiatorOutletTemperatureC,
    ignitionTimingCylinder1Deg = ignitionTimingCylinder1Deg,
    injectionTimeBank1Ms = injectionTimeBank1Ms,
    idleSpeedIntegratorPct = idleSpeedIntegratorPct,
    idleAirControlDutyPct = idleAirControlDutyPct,
    intakeCamshaftAngleDeg = intakeCamshaftAngleDeg,
    exhaustCamshaftAngleDeg = exhaustCamshaftAngleDeg,
    terminal15VoltageV = terminal15VoltageV,
    lambdaIntegratorBank1 = lambdaIntegratorBank1,
    lambdaIntegratorBank2 = lambdaIntegratorBank2,
    oxygenSensorHeaterUpstreamBank1DutyPct = oxygenSensorHeaterUpstreamBank1DutyPct,
    oxygenSensorHeaterUpstreamBank2DutyPct = oxygenSensorHeaterUpstreamBank2DutyPct,
    oxygenSensorHeaterDownstreamBank1DutyPct = oxygenSensorHeaterDownstreamBank1DutyPct,
    oxygenSensorHeaterDownstreamBank2DutyPct = oxygenSensorHeaterDownstreamBank2DutyPct,
    airMassPerStrokeMg = airMassPerStrokeMg,
    knockSignalCylinder2V = knockSignalCylinder2V,
    knockSignalCylinder5V = knockSignalCylinder5V,
    coolingFanDutyPct = coolingFanDutyPct,
    ambientPressureHpa = ambientPressureHpa,
    batteryVoltageV = batteryVoltageV,
)

internal fun dev.coldcat.dmelab.ffi.LiveVanos.toModel(): LiveVanos = LiveVanos(
    intake = intake.toModel(),
    exhaust = exhaust.toModel(),
)

internal fun dev.coldcat.dmelab.ffi.LiveDigital.toModel(): LiveDigital = LiveDigital(
    disaCommandActive = disaCommandActive,
)

internal fun dev.coldcat.dmelab.ffi.VanosPosition.toModel(): VanosPosition = VanosPosition(
    actualDeg = actualDeg,
    targetDeg = targetDeg,
    referenceDeg = referenceDeg,
)

internal fun dev.coldcat.dmelab.ffi.Progress.toModel(): Progress = Progress(
    stage = stage,
    completed = completed,
    total = total,
)

internal fun dev.coldcat.dmelab.ffi.SelectedLiveData.toModel(): SelectedLiveData = SelectedLiveData(
    measurements = measurements?.toModel(),
    vanos = vanos?.toModel(),
    digital = digital?.toModel(),
)
