//! Additional stock MS430DS0 blocks. Offsets include the three-byte DS2 header.

/// All 13 values in 0B 92; angles and adapted stop voltages are distinct.
#[derive(Debug, Clone, PartialEq)]
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

#[derive(Debug, Clone, PartialEq)]
pub struct CylinderRunning {
    pub cylinder: u8,
    /// Signed, dimensionless ER_QUO; not a compression measurement.
    pub roughness: f64,
    /// ECU counter, not a rate; accumulation/reset interval is not established.
    pub misfire_count: u16,
}

#[derive(Debug, Clone, PartialEq)]
pub struct LiveRoughRunning {
    /// Raw system-check status. The presence of values does not prove freshness.
    pub status: u8,
    /// Always six entries in cylinder-number order 1..6.
    pub cylinders: Vec<CylinderRunning>,
}

fn word(bytes: &[u8], offset: usize) -> u16 {
    u16::from_be_bytes([bytes[offset], bytes[offset + 1]])
}

fn signed_word(bytes: &[u8], offset: usize) -> i16 {
    i16::from_be_bytes([bytes[offset], bytes[offset + 1]])
}

// Full frame lengths (30 and 29) are checked by the live-block reader.
pub(super) fn decode_pedal(bytes: &[u8]) -> LivePedal {
    LivePedal {
        idle_adaptation_pct: f64::from(signed_word(bytes, 3)) * 0.001526,
        idle_adaptation_in_gear_pct: f64::from(signed_word(bytes, 5)) * 0.001526,
        idle_adaptation_ac_pct: f64::from(signed_word(bytes, 7)) * 0.001526,
        accelerator_pedal_sensor_1_angle_deg: f64::from(word(bytes, 9)) * 0.0018311,
        accelerator_pedal_sensor_2_angle_deg: f64::from(word(bytes, 11)) * 0.0018311,
        throttle_sensor_1_angle_deg: f64::from(word(bytes, 13)) * 0.007294,
        throttle_sensor_2_angle_deg: f64::from(word(bytes, 15)) * 0.007294,
        throttle_mechanical_stop_sensor_1_voltage_v: f64::from(word(bytes, 17)) * 0.0048828,
        throttle_mechanical_stop_sensor_2_voltage_v: f64::from(word(bytes, 19)) * 0.0048828,
        throttle_electrical_stop_sensor_1_voltage_v: f64::from(word(bytes, 21)) * 0.0048828,
        throttle_electrical_stop_sensor_2_voltage_v: f64::from(word(bytes, 23)) * 0.0048828,
        throttle_limp_home_sensor_1_voltage_v: f64::from(word(bytes, 25)) * 0.0048828,
        throttle_limp_home_sensor_2_voltage_v: f64::from(word(bytes, 27)) * 0.0048828,
    }
}

pub(super) fn decode_rough_running(bytes: &[u8]) -> LiveRoughRunning {
    // Wire order is 1, 5, 3, 6, 2, 4; expose explicit cylinder numbers in order.
    let offsets = [
        (1, 4, 16),
        (2, 12, 24),
        (3, 8, 20),
        (4, 14, 26),
        (5, 6, 18),
        (6, 10, 22),
    ];
    let cylinders = offsets
        .into_iter()
        .map(|(cylinder, rough, misfires)| CylinderRunning {
            cylinder,
            roughness: f64::from(signed_word(bytes, rough)) * 0.00122,
            misfire_count: word(bytes, misfires),
        })
        .collect();
    LiveRoughRunning {
        status: bytes[3],
        cylinders,
    }
}
