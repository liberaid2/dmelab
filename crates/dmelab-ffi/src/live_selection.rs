//! Selection and optional block records. Decoding belongs to dmelab-ms43.

use crate::{DmeError, LivePedal, LiveRoughRunning, Ms43Client, VanosPosition};

/// Which DS2 blocks to poll, always in measurements / VANOS / digital order.
/// At least one block must be selected. Existing read_live_data selects all three.
#[derive(Debug, Clone, Copy, uniffi::Record)]
pub struct LiveDataSelection {
    /// 0B 03: all 28 main measurements.
    pub measurements: bool,
    /// 0B 90: actual, target and reference angles for both camshafts.
    pub vanos: bool,
    /// 0B 04: digital state, currently the DISA command.
    pub digital: bool,
    /// 0B 92: pedal/throttle channels and adaptations.
    #[uniffi(default = false)]
    pub pedal: bool,
    /// 2B 05: cylinder roughness and misfire counters.
    #[uniffi(default = false)]
    pub rough_running: bool,
}

impl From<LiveDataSelection> for dmelab_ms43::LiveDataSelection {
    fn from(value: LiveDataSelection) -> Self {
        Self {
            measurements: value.measurements,
            vanos: value.vanos,
            digital: value.digital,
            pedal: value.pedal,
            rough_running: value.rough_running,
        }
    }
}

/// All measurements from a single 0B 03 response.
#[derive(Debug, Clone, uniffi::Record)]
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

#[derive(Debug, Clone, uniffi::Record)]
pub struct LiveVanos {
    pub intake: VanosPosition,
    pub exhaust: VanosPosition,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct LiveDigital {
    /// Command only; physical flap position is not measured.
    pub disa_command_active: bool,
}

/// One successful read of every selected block. Unselected blocks are None.
/// Values from failed or previous samples are never substituted.
#[derive(Debug, Clone, uniffi::Record)]
pub struct SelectedLiveData {
    pub measurements: Option<LiveMeasurements>,
    pub vanos: Option<LiveVanos>,
    pub digital: Option<LiveDigital>,
    pub pedal: Option<LivePedal>,
    pub rough_running: Option<LiveRoughRunning>,
}

impl From<dmelab_ms43::LiveMeasurements> for LiveMeasurements {
    fn from(value: dmelab_ms43::LiveMeasurements) -> Self {
        Self {
            engine_speed_rpm: value.engine_speed_rpm,
            vehicle_speed_kmh: value.vehicle_speed_kmh,
            accelerator_pedal_angle_deg: value.accelerator_pedal_angle_deg,
            throttle_angle_deg: value.throttle_angle_deg,
            mass_airflow_kgh: value.mass_airflow_kgh,
            intake_air_temperature_c: value.intake_air_temperature_c,
            coolant_temperature_c: value.coolant_temperature_c,
            oil_temperature_c: value.oil_temperature_c,
            radiator_outlet_temperature_c: value.radiator_outlet_temperature_c,
            ignition_timing_cylinder_1_deg: value.ignition_timing_cylinder_1_deg,
            injection_time_bank_1_ms: value.injection_time_bank_1_ms,
            idle_speed_integrator_pct: value.idle_speed_integrator_pct,
            idle_air_control_duty_pct: value.idle_air_control_duty_pct,
            intake_camshaft_angle_deg: value.intake_camshaft_angle_deg,
            exhaust_camshaft_angle_deg: value.exhaust_camshaft_angle_deg,
            terminal_15_voltage_v: value.terminal_15_voltage_v,
            lambda_integrator_bank_1: value.lambda_integrator_bank_1,
            lambda_integrator_bank_2: value.lambda_integrator_bank_2,
            oxygen_sensor_heater_upstream_bank_1_duty_pct: value
                .oxygen_sensor_heater_upstream_bank_1_duty_pct,
            oxygen_sensor_heater_upstream_bank_2_duty_pct: value
                .oxygen_sensor_heater_upstream_bank_2_duty_pct,
            oxygen_sensor_heater_downstream_bank_1_duty_pct: value
                .oxygen_sensor_heater_downstream_bank_1_duty_pct,
            oxygen_sensor_heater_downstream_bank_2_duty_pct: value
                .oxygen_sensor_heater_downstream_bank_2_duty_pct,
            air_mass_per_stroke_mg: value.air_mass_per_stroke_mg,
            knock_signal_cylinder_2_v: value.knock_signal_cylinder_2_v,
            knock_signal_cylinder_5_v: value.knock_signal_cylinder_5_v,
            cooling_fan_duty_pct: value.cooling_fan_duty_pct,
            ambient_pressure_hpa: value.ambient_pressure_hpa,
            battery_voltage_v: value.battery_voltage_v,
        }
    }
}

impl From<dmelab_ms43::LiveVanos> for LiveVanos {
    fn from(value: dmelab_ms43::LiveVanos) -> Self {
        Self {
            intake: value.intake.into(),
            exhaust: value.exhaust.into(),
        }
    }
}

impl From<dmelab_ms43::LiveDigital> for LiveDigital {
    fn from(value: dmelab_ms43::LiveDigital) -> Self {
        Self {
            disa_command_active: value.disa_command_active,
        }
    }
}

impl From<dmelab_ms43::SelectedLiveData> for SelectedLiveData {
    fn from(value: dmelab_ms43::SelectedLiveData) -> Self {
        Self {
            measurements: value.measurements.map(Into::into),
            vanos: value.vanos.map(Into::into),
            digital: value.digital.map(Into::into),
            pedal: value.pedal.map(Into::into),
            rough_running: value.rough_running.map(Into::into),
        }
    }
}

#[uniffi::export]
impl Ms43Client {
    /// Reject an empty selection before configuring the transport.
    pub fn read_live_data_selected(
        &self,
        selection: LiveDataSelection,
        timeout_ms: u32,
    ) -> Result<SelectedLiveData, DmeError> {
        let selection = dmelab_ms43::LiveDataSelection::from(selection);
        selection.validate()?;
        self.execute(timeout_ms, |session| {
            session.read_live_data_selected(selection).map(Into::into)
        })
    }
}
