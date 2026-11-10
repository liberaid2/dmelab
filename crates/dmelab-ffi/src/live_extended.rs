//! Typed records for 0B 92 and 2B 05.

/// All 13 values in 0B 92; angles and adapted stop voltages are distinct.
#[derive(Debug, Clone, uniffi::Record)]
pub struct LivePedal {
    pub idle_adaptation_pct: f64,
    pub idle_adaptation_in_gear_pct: f64,
    pub idle_adaptation_ac_pct: f64,
    pub accelerator_pedal_sensor_1_angle_deg: f64,
    pub accelerator_pedal_sensor_2_angle_deg: f64,
    pub throttle_sensor_1_angle_deg: f64,
    pub throttle_sensor_2_angle_deg: f64,
    pub throttle_mechanical_stop_sensor_1_voltage_v: f64,
    pub throttle_mechanical_stop_sensor_2_voltage_v: f64,
    pub throttle_electrical_stop_sensor_1_voltage_v: f64,
    pub throttle_electrical_stop_sensor_2_voltage_v: f64,
    pub throttle_limp_home_sensor_1_voltage_v: f64,
    pub throttle_limp_home_sensor_2_voltage_v: f64,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct CylinderRunning {
    pub cylinder: u8,
    /// Signed, dimensionless ER_QUO; not a compression measurement.
    pub roughness: f64,
    /// ECU counter, not a rate; accumulation/reset interval is not established.
    pub misfire_count: u16,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct LiveRoughRunning {
    /// Raw system-check status. The presence of values does not prove freshness.
    pub status: u8,
    /// Always six entries in cylinder-number order 1..6.
    pub cylinders: Vec<CylinderRunning>,
}

impl From<dmelab_ms43::LivePedal> for LivePedal {
    fn from(value: dmelab_ms43::LivePedal) -> Self {
        Self {
            idle_adaptation_pct: value.idle_adaptation_pct,
            idle_adaptation_in_gear_pct: value.idle_adaptation_in_gear_pct,
            idle_adaptation_ac_pct: value.idle_adaptation_ac_pct,
            accelerator_pedal_sensor_1_angle_deg: value.accelerator_pedal_sensor_1_angle_deg,
            accelerator_pedal_sensor_2_angle_deg: value.accelerator_pedal_sensor_2_angle_deg,
            throttle_sensor_1_angle_deg: value.throttle_sensor_1_angle_deg,
            throttle_sensor_2_angle_deg: value.throttle_sensor_2_angle_deg,
            throttle_mechanical_stop_sensor_1_voltage_v: value
                .throttle_mechanical_stop_sensor_1_voltage_v,
            throttle_mechanical_stop_sensor_2_voltage_v: value
                .throttle_mechanical_stop_sensor_2_voltage_v,
            throttle_electrical_stop_sensor_1_voltage_v: value
                .throttle_electrical_stop_sensor_1_voltage_v,
            throttle_electrical_stop_sensor_2_voltage_v: value
                .throttle_electrical_stop_sensor_2_voltage_v,
            throttle_limp_home_sensor_1_voltage_v: value.throttle_limp_home_sensor_1_voltage_v,
            throttle_limp_home_sensor_2_voltage_v: value.throttle_limp_home_sensor_2_voltage_v,
        }
    }
}

impl From<dmelab_ms43::CylinderRunning> for CylinderRunning {
    fn from(value: dmelab_ms43::CylinderRunning) -> Self {
        Self {
            cylinder: value.cylinder,
            roughness: value.roughness,
            misfire_count: value.misfire_count,
        }
    }
}

impl From<dmelab_ms43::LiveRoughRunning> for LiveRoughRunning {
    fn from(value: dmelab_ms43::LiveRoughRunning) -> Self {
        Self {
            status: value.status,
            cylinders: value.cylinders.into_iter().map(Into::into).collect(),
        }
    }
}
