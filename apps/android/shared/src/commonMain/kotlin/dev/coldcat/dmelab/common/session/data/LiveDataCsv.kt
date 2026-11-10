package dev.coldcat.dmelab.common.session.data

import dev.coldcat.dmelab.common.ecu.model.LiveDataSelection
import dev.coldcat.dmelab.common.ecu.model.LiveMeasurements
import dev.coldcat.dmelab.common.ecu.model.SelectedLiveData
import kotlin.time.Instant

/** Fixed schema per recording; each column owns its name and accessor together. */
class LiveDataCsv(private val selection: LiveDataSelection) {
    init { require(selection.isNotEmpty) }

    private val columns = buildList {
        if (selection.measurements) addAll(listOf(
            measurement("engine_speed_rpm") { it.engineSpeedRpm },
            measurement("vehicle_speed_kmh") { it.vehicleSpeedKmh },
            measurement("accelerator_pedal_angle_deg") { it.acceleratorPedalAngleDeg },
            measurement("throttle_angle_deg") { it.throttleAngleDeg },
            measurement("mass_airflow_kg_h") { it.massAirflowKgh },
            measurement("intake_air_temperature_c") { it.intakeAirTemperatureC },
            measurement("coolant_temperature_c") { it.coolantTemperatureC },
            measurement("oil_temperature_c") { it.oilTemperatureC },
            measurement("radiator_outlet_temperature_c") { it.radiatorOutletTemperatureC },
            measurement("ignition_timing_cylinder_1_deg") { it.ignitionTimingCylinder1Deg },
            measurement("injection_time_bank_1_ms") { it.injectionTimeBank1Ms },
            measurement("idle_speed_integrator_pct") { it.idleSpeedIntegratorPct },
            measurement("idle_air_control_duty_pct") { it.idleAirControlDutyPct },
            measurement("intake_camshaft_angle_deg") { it.intakeCamshaftAngleDeg },
            measurement("exhaust_camshaft_angle_deg") { it.exhaustCamshaftAngleDeg },
            measurement("terminal_15_voltage_v") { it.terminal15VoltageV },
            measurement("lambda_integrator_bank_1") { it.lambdaIntegratorBank1 },
            measurement("lambda_integrator_bank_2") { it.lambdaIntegratorBank2 },
            measurement("oxygen_sensor_heater_upstream_bank_1_duty_pct") { it.oxygenSensorHeaterUpstreamBank1DutyPct },
            measurement("oxygen_sensor_heater_upstream_bank_2_duty_pct") { it.oxygenSensorHeaterUpstreamBank2DutyPct },
            measurement("oxygen_sensor_heater_downstream_bank_1_duty_pct") { it.oxygenSensorHeaterDownstreamBank1DutyPct },
            measurement("oxygen_sensor_heater_downstream_bank_2_duty_pct") { it.oxygenSensorHeaterDownstreamBank2DutyPct },
            measurement("air_mass_per_stroke_mg") { it.airMassPerStrokeMg },
            measurement("knock_signal_cylinder_2_v") { it.knockSignalCylinder2V },
            measurement("knock_signal_cylinder_5_v") { it.knockSignalCylinder5V },
            measurement("cooling_fan_duty_pct") { it.coolingFanDutyPct },
            measurement("ambient_pressure_hpa") { it.ambientPressureHpa },
            measurement("battery_voltage_v") { it.batteryVoltageV },
        ))
        if (selection.vanos) addAll(listOf(
            Column("intake_vanos_actual_deg") { checkNotNull(it.vanos).intake.actualDeg },
            Column("intake_vanos_target_deg") { checkNotNull(it.vanos).intake.targetDeg },
            Column("intake_vanos_reference_deg") { checkNotNull(it.vanos).intake.referenceDeg },
            Column("exhaust_vanos_actual_deg") { checkNotNull(it.vanos).exhaust.actualDeg },
            Column("exhaust_vanos_target_deg") { checkNotNull(it.vanos).exhaust.targetDeg },
            Column("exhaust_vanos_reference_deg") { checkNotNull(it.vanos).exhaust.referenceDeg },
        ))
        if (selection.digital) add(Column("disa_command_active") { checkNotNull(it.digital).disaCommandActive })
    }
    val header = (listOf("timestamp_utc", "elapsed_ms") + columns.map { it.name }).joinToString(separator = ",")

    // Only numbers, booleans and an ISO timestamp: none contain CSV delimiters.
    fun row(data: SelectedLiveData, timestampMillis: Long, elapsedMillis: Long): String {
        check((data.measurements != null) == selection.measurements &&
            (data.vanos != null) == selection.vanos && (data.digital != null) == selection.digital)
        return (listOf(Instant.fromEpochMilliseconds(timestampMillis), elapsedMillis) + columns.map { it.read(data) })
            .joinToString(separator = ",")
    }

    private fun measurement(name: String, read: (LiveMeasurements) -> Any) =
        Column(name = name, read = { read(checkNotNull(it.measurements)) })

    private data class Column(val name: String, val read: (SelectedLiveData) -> Any)
}
