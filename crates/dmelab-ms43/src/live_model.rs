//! Typed live-data models; the flat full result is retained for existing clients.

use crate::{Error, LivePedal, LiveRoughRunning};

/// Angles in crankshaft degrees in the MS43 coordinate system.
/// All three values come from the same 0B 90 response.
#[derive(Debug, Clone, Copy, PartialEq)]
pub struct VanosPosition {
    pub actual_deg: f64,
    pub target_deg: f64,
    pub reference_deg: f64,
}

/// One complete sequential read of three blocks, not an atomic ECU snapshot.
#[derive(Debug, Clone, PartialEq)]
pub struct LiveData {
    pub engine_speed_rpm: u16,
    pub vehicle_speed_kmh: u8,
    pub accelerator_pedal_angle_deg: f64,
    pub throttle_angle_deg: f64,
    pub mass_airflow_kgh: f64,
    pub intake_air_temperature_c: f64,
    pub coolant_temperature_c: f64,
    pub oil_temperature_c: f64,
    pub radiator_outlet_temperature_c: f64,
    pub ignition_timing_cylinder_1_deg: f64,
    pub injection_time_bank_1_ms: f64,
    /// Signed idle-speed controller integral correction, not valve duty.
    pub idle_speed_integrator_pct: f64,
    pub idle_air_control_duty_pct: f64,
    /// Actual crankshaft-degree angles from 0B 03, sampled before 0B 90.
    pub intake_camshaft_angle_deg: f64,
    pub exhaust_camshaft_angle_deg: f64,
    pub terminal_15_voltage_v: f64,
    /// Multiplicative correction factor, not measured exhaust lambda or AFR.
    pub lambda_integrator_bank_1: f64,
    pub lambda_integrator_bank_2: f64,
    /// Commanded oxygen-sensor heater duties, not measured temperatures.
    pub oxygen_sensor_heater_upstream_bank_1_duty_pct: f64,
    pub oxygen_sensor_heater_upstream_bank_2_duty_pct: f64,
    pub oxygen_sensor_heater_downstream_bank_1_duty_pct: f64,
    pub oxygen_sensor_heater_downstream_bank_2_duty_pct: f64,
    pub air_mass_per_stroke_mg: f64,
    /// Mean knock signal voltage, not a knock event counter.
    pub knock_signal_cylinder_2_v: f64,
    pub knock_signal_cylinder_5_v: f64,
    /// Commanded fan PWM duty, not measured fan speed.
    pub cooling_fan_duty_pct: f64,
    /// Ambient pressure from AMP_MES, not calculated manifold pressure (MAP).
    pub ambient_pressure_hpa: f64,
    pub battery_voltage_v: f64,
    pub intake_vanos: VanosPosition,
    pub exhaust_vanos: VanosPosition,
    /// Actuation command only; physical flap position is not measured here.
    pub disa_command_active: bool,
}

/// Which DS2 blocks to poll, always in measurements / VANOS / digital order.
/// Default retains the original three blocks. At least one block must be selected.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct LiveDataSelection {
    /// 0B 03: all 28 main measurements.
    pub measurements: bool,
    /// 0B 90: actual, target and reference angles for both camshafts.
    pub vanos: bool,
    /// 0B 04: digital state, currently the DISA command.
    pub digital: bool,
    /// 0B 92: pedal/throttle channels, idle and throttle-stop adaptations.
    pub pedal: bool,
    /// 2B 05: system-check status, cylinder roughness and misfire counters.
    pub rough_running: bool,
}

impl Default for LiveDataSelection {
    fn default() -> Self {
        Self {
            measurements: true,
            vanos: true,
            digital: true,
            pedal: false,
            rough_running: false,
        }
    }
}

impl LiveDataSelection {
    /// Select all five supported blocks, including the optional extended blocks.
    pub fn all() -> Self {
        Self {
            pedal: true,
            rough_running: true,
            ..Self::default()
        }
    }

    /// Validate before configuring the adapter or identifying the ECU.
    pub fn validate(self) -> Result<(), Error> {
        if self.measurements || self.vanos || self.digital || self.pedal || self.rough_running {
            Ok(())
        } else {
            Err(Error::EmptyLiveDataSelection)
        }
    }
}

/// All measurements from a single 0B 03 response.
#[derive(Debug, Clone, PartialEq)]
pub struct LiveMeasurements {
    pub engine_speed_rpm: u16,
    pub vehicle_speed_kmh: u8,
    pub accelerator_pedal_angle_deg: f64,
    pub throttle_angle_deg: f64,
    pub mass_airflow_kgh: f64,
    pub intake_air_temperature_c: f64,
    pub coolant_temperature_c: f64,
    pub oil_temperature_c: f64,
    pub radiator_outlet_temperature_c: f64,
    pub ignition_timing_cylinder_1_deg: f64,
    pub injection_time_bank_1_ms: f64,
    /// Signed idle-speed controller integral correction, not valve duty.
    pub idle_speed_integrator_pct: f64,
    pub idle_air_control_duty_pct: f64,
    /// Actual crankshaft-degree angles from 0B 03, sampled before 0B 90.
    pub intake_camshaft_angle_deg: f64,
    pub exhaust_camshaft_angle_deg: f64,
    pub terminal_15_voltage_v: f64,
    /// Multiplicative correction factor, not measured exhaust lambda or AFR.
    pub lambda_integrator_bank_1: f64,
    pub lambda_integrator_bank_2: f64,
    /// Commanded oxygen-sensor heater duties, not measured temperatures.
    pub oxygen_sensor_heater_upstream_bank_1_duty_pct: f64,
    pub oxygen_sensor_heater_upstream_bank_2_duty_pct: f64,
    pub oxygen_sensor_heater_downstream_bank_1_duty_pct: f64,
    pub oxygen_sensor_heater_downstream_bank_2_duty_pct: f64,
    pub air_mass_per_stroke_mg: f64,
    /// Mean knock signal voltage, not a knock event counter.
    pub knock_signal_cylinder_2_v: f64,
    pub knock_signal_cylinder_5_v: f64,
    /// Commanded fan PWM duty, not measured fan speed.
    pub cooling_fan_duty_pct: f64,
    /// Ambient pressure from AMP_MES, not calculated manifold pressure (MAP).
    pub ambient_pressure_hpa: f64,
    pub battery_voltage_v: f64,
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub struct LiveVanos {
    pub intake: VanosPosition,
    pub exhaust: VanosPosition,
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub struct LiveDigital {
    /// Command only; physical flap position is not measured.
    pub disa_command_active: bool,
}

/// One successful read of every selected block. Unselected blocks are None.
/// Values from failed or previous samples are never substituted.
#[derive(Debug, Clone, PartialEq)]
pub struct SelectedLiveData {
    pub measurements: Option<LiveMeasurements>,
    pub vanos: Option<LiveVanos>,
    pub digital: Option<LiveDigital>,
    pub pedal: Option<LivePedal>,
    pub rough_running: Option<LiveRoughRunning>,
}

impl TryFrom<SelectedLiveData> for LiveData {
    type Error = Error;

    fn try_from(data: SelectedLiveData) -> Result<Self, Error> {
        let m = data.measurements.ok_or(Error::IncompleteLiveData)?;
        let v = data.vanos.ok_or(Error::IncompleteLiveData)?;
        let d = data.digital.ok_or(Error::IncompleteLiveData)?;
        Ok(Self {
            engine_speed_rpm: m.engine_speed_rpm,
            vehicle_speed_kmh: m.vehicle_speed_kmh,
            accelerator_pedal_angle_deg: m.accelerator_pedal_angle_deg,
            throttle_angle_deg: m.throttle_angle_deg,
            mass_airflow_kgh: m.mass_airflow_kgh,
            intake_air_temperature_c: m.intake_air_temperature_c,
            coolant_temperature_c: m.coolant_temperature_c,
            oil_temperature_c: m.oil_temperature_c,
            radiator_outlet_temperature_c: m.radiator_outlet_temperature_c,
            ignition_timing_cylinder_1_deg: m.ignition_timing_cylinder_1_deg,
            injection_time_bank_1_ms: m.injection_time_bank_1_ms,
            idle_speed_integrator_pct: m.idle_speed_integrator_pct,
            idle_air_control_duty_pct: m.idle_air_control_duty_pct,
            intake_camshaft_angle_deg: m.intake_camshaft_angle_deg,
            exhaust_camshaft_angle_deg: m.exhaust_camshaft_angle_deg,
            terminal_15_voltage_v: m.terminal_15_voltage_v,
            lambda_integrator_bank_1: m.lambda_integrator_bank_1,
            lambda_integrator_bank_2: m.lambda_integrator_bank_2,
            oxygen_sensor_heater_upstream_bank_1_duty_pct: m
                .oxygen_sensor_heater_upstream_bank_1_duty_pct,
            oxygen_sensor_heater_upstream_bank_2_duty_pct: m
                .oxygen_sensor_heater_upstream_bank_2_duty_pct,
            oxygen_sensor_heater_downstream_bank_1_duty_pct: m
                .oxygen_sensor_heater_downstream_bank_1_duty_pct,
            oxygen_sensor_heater_downstream_bank_2_duty_pct: m
                .oxygen_sensor_heater_downstream_bank_2_duty_pct,
            air_mass_per_stroke_mg: m.air_mass_per_stroke_mg,
            knock_signal_cylinder_2_v: m.knock_signal_cylinder_2_v,
            knock_signal_cylinder_5_v: m.knock_signal_cylinder_5_v,
            cooling_fan_duty_pct: m.cooling_fan_duty_pct,
            ambient_pressure_hpa: m.ambient_pressure_hpa,
            battery_voltage_v: m.battery_voltage_v,
            intake_vanos: v.intake,
            exhaust_vanos: v.exhaust,
            disa_command_active: d.disa_command_active,
        })
    }
}
