//! UniFFI records for a complete live data read. Conversion only; no decoding.

#[derive(Debug, Clone, uniffi::Record)]
pub struct VanosPosition {
    pub actual_deg: f64,
    pub target_deg: f64,
    pub reference_deg: f64,
}

impl From<dmelab_ms43::VanosPosition> for VanosPosition {
    fn from(value: dmelab_ms43::VanosPosition) -> Self {
        Self {
            actual_deg: value.actual_deg,
            target_deg: value.target_deg,
            reference_deg: value.reference_deg,
        }
    }
}

#[derive(Debug, Clone, uniffi::Record)]
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
    pub idle_speed_integrator_pct: f64,
    pub idle_air_control_duty_pct: f64,
    /// Actual angles from 0B 03; intake_vanos/exhaust_vanos use 0B 90.
    pub intake_camshaft_angle_deg: f64,
    pub exhaust_camshaft_angle_deg: f64,
    pub terminal_15_voltage_v: f64,
    /// Multiplicative correction factor, not measured exhaust lambda or AFR.
    pub lambda_integrator_bank_1: f64,
    pub lambda_integrator_bank_2: f64,
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
    pub ambient_pressure_hpa: f64,
    pub battery_voltage_v: f64,
    pub intake_vanos: VanosPosition,
    pub exhaust_vanos: VanosPosition,
    /// Actuation command only; physical flap position is not measured here.
    pub disa_command_active: bool,
}

impl From<dmelab_ms43::LiveData> for LiveData {
    fn from(value: dmelab_ms43::LiveData) -> Self {
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
            intake_vanos: value.intake_vanos.into(),
            exhaust_vanos: value.exhaust_vanos.into(),
            disa_command_active: value.disa_command_active,
        }
    }
}
