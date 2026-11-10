package dev.coldcat.dmelab.common.session.data

import dev.coldcat.dmelab.common.ecu.model.SelectedLiveData
import dev.coldcat.dmelab.common.resources.AppStrings
import dev.coldcat.dmelab.common.session.domain.LiveValue
import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.live_air_mass
import dev.coldcat.dmelab.shared.resources.live_airflow
import dev.coldcat.dmelab.shared.resources.live_ambient_pressure
import dev.coldcat.dmelab.shared.resources.live_battery_voltage
import dev.coldcat.dmelab.shared.resources.live_coolant_temperature
import dev.coldcat.dmelab.shared.resources.live_disa
import dev.coldcat.dmelab.shared.resources.live_engine_speed
import dev.coldcat.dmelab.shared.resources.live_exhaust_actual
import dev.coldcat.dmelab.shared.resources.live_exhaust_camshaft
import dev.coldcat.dmelab.shared.resources.live_exhaust_reference
import dev.coldcat.dmelab.shared.resources.live_exhaust_target
import dev.coldcat.dmelab.shared.resources.live_fan
import dev.coldcat.dmelab.shared.resources.live_heater_downstream1
import dev.coldcat.dmelab.shared.resources.live_heater_downstream2
import dev.coldcat.dmelab.shared.resources.live_heater_upstream1
import dev.coldcat.dmelab.shared.resources.live_heater_upstream2
import dev.coldcat.dmelab.shared.resources.live_idle_duty
import dev.coldcat.dmelab.shared.resources.live_idle_integrator
import dev.coldcat.dmelab.shared.resources.live_ignition
import dev.coldcat.dmelab.shared.resources.live_injection
import dev.coldcat.dmelab.shared.resources.live_intake_actual
import dev.coldcat.dmelab.shared.resources.live_intake_camshaft
import dev.coldcat.dmelab.shared.resources.live_intake_reference
import dev.coldcat.dmelab.shared.resources.live_intake_target
import dev.coldcat.dmelab.shared.resources.live_intake_temperature
import dev.coldcat.dmelab.shared.resources.live_knock2
import dev.coldcat.dmelab.shared.resources.live_knock5
import dev.coldcat.dmelab.shared.resources.live_lambda1
import dev.coldcat.dmelab.shared.resources.live_lambda2
import dev.coldcat.dmelab.shared.resources.live_oil_temperature
import dev.coldcat.dmelab.shared.resources.live_pedal
import dev.coldcat.dmelab.shared.resources.live_radiator_temperature
import dev.coldcat.dmelab.shared.resources.live_terminal_15
import dev.coldcat.dmelab.shared.resources.live_throttle
import dev.coldcat.dmelab.shared.resources.live_unit_celsius
import dev.coldcat.dmelab.shared.resources.live_unit_degrees
import dev.coldcat.dmelab.shared.resources.live_unit_factor
import dev.coldcat.dmelab.shared.resources.live_unit_hpa
import dev.coldcat.dmelab.shared.resources.live_unit_kgh
import dev.coldcat.dmelab.shared.resources.live_unit_kmh
import dev.coldcat.dmelab.shared.resources.live_unit_mgstroke
import dev.coldcat.dmelab.shared.resources.live_unit_ms
import dev.coldcat.dmelab.shared.resources.live_unit_percent
import dev.coldcat.dmelab.shared.resources.live_unit_rpm
import dev.coldcat.dmelab.shared.resources.live_unit_volts
import dev.coldcat.dmelab.shared.resources.live_vehicle_speed
import org.jetbrains.compose.resources.StringResource
import org.koin.core.annotation.Single

/** UI labels/units only; every measurement is decoded by Rust. */
@Single
internal class LiveDataValues(private val strings: AppStrings) {
    fun map(sample: SelectedLiveData): List<LiveValue> = buildList {
        sample.measurements?.let { data -> addAll(listOf(
            value(Res.string.live_engine_speed, data.engineSpeedRpm.toDouble(), Res.string.live_unit_rpm),
            value(Res.string.live_vehicle_speed, data.vehicleSpeedKmh.toDouble(), Res.string.live_unit_kmh),
            value(Res.string.live_pedal, data.acceleratorPedalAngleDeg, Res.string.live_unit_degrees),
            value(Res.string.live_throttle, data.throttleAngleDeg, Res.string.live_unit_degrees),
            value(Res.string.live_airflow, data.massAirflowKgh, Res.string.live_unit_kgh),
            value(Res.string.live_intake_temperature, data.intakeAirTemperatureC, Res.string.live_unit_celsius),
            value(Res.string.live_coolant_temperature, data.coolantTemperatureC, Res.string.live_unit_celsius),
            value(Res.string.live_oil_temperature, data.oilTemperatureC, Res.string.live_unit_celsius),
            value(Res.string.live_radiator_temperature, data.radiatorOutletTemperatureC, Res.string.live_unit_celsius),
            value(Res.string.live_ignition, data.ignitionTimingCylinder1Deg, Res.string.live_unit_degrees),
            value(Res.string.live_injection, data.injectionTimeBank1Ms, Res.string.live_unit_ms),
            value(Res.string.live_lambda1, data.lambdaIntegratorBank1, Res.string.live_unit_factor),
            value(Res.string.live_lambda2, data.lambdaIntegratorBank2, Res.string.live_unit_factor),
            value(Res.string.live_air_mass, data.airMassPerStrokeMg, Res.string.live_unit_mgstroke),
            value(Res.string.live_knock2, data.knockSignalCylinder2V, Res.string.live_unit_volts),
            value(Res.string.live_knock5, data.knockSignalCylinder5V, Res.string.live_unit_volts),
            value(Res.string.live_fan, data.coolingFanDutyPct, Res.string.live_unit_percent),
            value(Res.string.live_idle_integrator, data.idleSpeedIntegratorPct, Res.string.live_unit_percent),
            value(Res.string.live_idle_duty, data.idleAirControlDutyPct, Res.string.live_unit_percent),
            value(Res.string.live_intake_camshaft, data.intakeCamshaftAngleDeg, Res.string.live_unit_degrees),
            value(Res.string.live_exhaust_camshaft, data.exhaustCamshaftAngleDeg, Res.string.live_unit_degrees),
            value(Res.string.live_terminal_15, data.terminal15VoltageV, Res.string.live_unit_volts),
            value(Res.string.live_heater_upstream1, data.oxygenSensorHeaterUpstreamBank1DutyPct, Res.string.live_unit_percent),
            value(Res.string.live_heater_upstream2, data.oxygenSensorHeaterUpstreamBank2DutyPct, Res.string.live_unit_percent),
            value(Res.string.live_heater_downstream1, data.oxygenSensorHeaterDownstreamBank1DutyPct, Res.string.live_unit_percent),
            value(Res.string.live_heater_downstream2, data.oxygenSensorHeaterDownstreamBank2DutyPct, Res.string.live_unit_percent),
            value(Res.string.live_ambient_pressure, data.ambientPressureHpa, Res.string.live_unit_hpa),
            value(Res.string.live_battery_voltage, data.batteryVoltageV, Res.string.live_unit_volts),
        )) }
        sample.vanos?.let { data -> addAll(listOf(
            value(Res.string.live_intake_actual, data.intake.actualDeg, Res.string.live_unit_degrees),
            value(Res.string.live_intake_target, data.intake.targetDeg, Res.string.live_unit_degrees),
            value(Res.string.live_intake_reference, data.intake.referenceDeg, Res.string.live_unit_degrees),
            value(Res.string.live_exhaust_actual, data.exhaust.actualDeg, Res.string.live_unit_degrees),
            value(Res.string.live_exhaust_target, data.exhaust.targetDeg, Res.string.live_unit_degrees),
            value(Res.string.live_exhaust_reference, data.exhaust.referenceDeg, Res.string.live_unit_degrees),
        )) }
        sample.digital?.let { data ->
            add(LiveValue(label = strings.get(Res.string.live_disa), value = null, unit = "", flag = data.disaCommandActive))
        }
    }

    private fun value(label: StringResource, value: Double, unit: StringResource) = LiveValue(label = strings.get(label), value = value, unit = strings.get(unit))
}
