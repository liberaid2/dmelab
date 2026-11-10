use std::io::{self, Write};

use dmelab_ms43::{LivePedal, LiveRoughRunning};

pub(crate) fn print_pedal(output: &mut impl Write, data: &LivePedal) -> io::Result<()> {
    writeln!(
        output,
        "Idle adaptations: base={:.4} %, in gear={:.4} %, AC={:.4} %",
        data.idle_adaptation_pct, data.idle_adaptation_in_gear_pct, data.idle_adaptation_ac_pct
    )?;
    writeln!(
        output,
        "Pedal sensor angles: 1={:.4}, 2={:.4} deg",
        data.accelerator_pedal_sensor_1_angle_deg, data.accelerator_pedal_sensor_2_angle_deg
    )?;
    writeln!(
        output,
        "Throttle sensor angles: 1={:.4}, 2={:.4} deg",
        data.throttle_sensor_1_angle_deg, data.throttle_sensor_2_angle_deg
    )?;
    writeln!(
        output,
        "Throttle mechanical stop: sensor 1={:.4}, sensor 2={:.4} V",
        data.throttle_mechanical_stop_sensor_1_voltage_v,
        data.throttle_mechanical_stop_sensor_2_voltage_v
    )?;
    writeln!(
        output,
        "Throttle electrical stop: sensor 1={:.4}, sensor 2={:.4} V",
        data.throttle_electrical_stop_sensor_1_voltage_v,
        data.throttle_electrical_stop_sensor_2_voltage_v
    )?;
    writeln!(
        output,
        "Throttle limp-home stop: sensor 1={:.4}, sensor 2={:.4} V",
        data.throttle_limp_home_sensor_1_voltage_v, data.throttle_limp_home_sensor_2_voltage_v
    )
}

pub(crate) fn print_rough_running(
    output: &mut impl Write,
    data: &LiveRoughRunning,
) -> io::Result<()> {
    writeln!(
        output,
        "Rough-running system-check status: 0x{:02X} (raw; freshness not established)",
        data.status
    )?;
    for cylinder in &data.cylinders {
        writeln!(
            output,
            "Cylinder {}: roughness={:.5}, misfire counter={}",
            cylinder.cylinder, cylinder.roughness, cylinder.misfire_count
        )?;
    }
    Ok(())
}
