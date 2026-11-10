use std::collections::VecDeque;
use std::io::{self, Read, Write};

use dmelab_core::Transport;

use super::*;
use crate::BaudRate;
use crate::protocol::Protocol;

struct ScriptedTransport {
    exchanges: VecDeque<(u8, Vec<u8>)>,
    incoming: VecDeque<u8>,
}

impl Transport for ScriptedTransport {
    fn set_timeout(&mut self, timeout: Duration) -> io::Result<()> {
        assert!(!timeout.is_zero());
        Ok(())
    }
    fn set_baud_rate(&mut self, _: u32) -> io::Result<()> {
        panic!("live reads must not change speed");
    }
    fn clear_buffers(&mut self) -> io::Result<()> {
        assert!(self.incoming.is_empty(), "unread response was discarded");
        Ok(())
    }
}

impl Write for ScriptedTransport {
    fn write(&mut self, bytes: &[u8]) -> io::Result<usize> {
        let (block, response) = self.exchanges.pop_front().expect("unexpected request");
        assert_eq!(bytes, [0x12, 5, 0x0B, block, 0x12 ^ 5 ^ 0x0B ^ block]);
        self.incoming.extend(bytes);
        self.incoming.extend(response);
        Ok(bytes.len())
    }
    fn flush(&mut self) -> io::Result<()> {
        Ok(())
    }
}

impl Read for ScriptedTransport {
    fn read(&mut self, bytes: &mut [u8]) -> io::Result<usize> {
        let count = bytes.len().min(3).min(self.incoming.len());
        for byte in &mut bytes[..count] {
            *byte = self.incoming.pop_front().unwrap();
        }
        Ok(count)
    }
}

impl Drop for ScriptedTransport {
    fn drop(&mut self) {
        if !std::thread::panicking() {
            assert!(self.exchanges.is_empty(), "expected request was not sent");
            assert!(self.incoming.is_empty(), "response was not consumed");
        }
    }
}

fn frame(data: &[u8]) -> Vec<u8> {
    let mut bytes = vec![0x12, u8::try_from(data.len() + 4).unwrap(), 0xA0];
    bytes.extend(data);
    bytes.push(bytes.iter().fold(0, |sum, b| sum ^ b));
    bytes
}

fn session(exchanges: Vec<(u8, Vec<u8>)>) -> Session {
    let transport = ScriptedTransport {
        exchanges: exchanges.into(),
        incoming: VecDeque::new(),
    };
    Session {
        protocol: Protocol::new(transport, Duration::from_secs(1)).unwrap(),
        state: LinkState::Ready(BaudRate::B9600),
    }
}

fn close(actual: f64, expected: f64) {
    assert!(
        (actual - expected).abs() < 0.0000001,
        "{actual} != {expected}"
    );
}

#[test]
fn three_requests_decode_all_measurements_and_both_vanos_from_one_block() {
    let mut measurements = [0; 41];
    // Positions below are relative to the payload; asymmetric words catch endianness.
    for (offset, value) in [
        (0, 0x1234_u16),
        (3, 0x1234),
        (5, 0x2345),
        (7, 400),
        (14, 1000),
        (16, (-1234_i16) as u16),
        (18, 0x3456),
        (23, 32768),
        (25, 16384),
        (31, 10000),
        (33, 1000),
        (35, 2000),
        (38, 0x2f34),
    ] {
        measurements[offset..offset + 2].copy_from_slice(&value.to_be_bytes());
    }
    measurements[2] = 42;
    measurements[9..14].copy_from_slice(&[32, 160, 180, 144, 160]);
    // 0B 03 angles must be separate from the later, coherent VANOS trio.
    measurements[20..23].copy_from_slice(&[1, 0xf6, 130]);
    measurements[27..31].copy_from_slice(&[10, 20, 30, 40]);
    measurements[37] = 128;
    measurements[40] = 135;
    let vanos = [120, 128, 0x12, 0x34, 0, 0, 80, 88, 0x23, 0x45, 0, 0];
    let mut session = session(vec![
        (3, frame(&measurements)),
        (0x90, frame(&vanos)),
        (4, frame(&[0, 0, 0, 4, 0, 0])),
    ]);
    let data = session.read_live_data().unwrap();
    assert_eq!(data.engine_speed_rpm, 0x1234);
    assert_eq!(data.vehicle_speed_kmh, 42);
    close(data.accelerator_pedal_angle_deg, 0x1234 as f64 * 0.0018311);
    close(data.throttle_angle_deg, 0x2345 as f64 * 0.0018311);
    close(data.mass_airflow_kgh, 100.0);
    close(data.intake_air_temperature_c, -24.0);
    close(data.coolant_temperature_c, 72.0);
    close(data.oil_temperature_c, 95.28);
    close(data.radiator_outlet_temperature_c, 60.0);
    close(data.ignition_timing_cylinder_1_deg, 12.0);
    close(data.injection_time_bank_1_ms, 5.3333);
    close(data.idle_speed_integrator_pct, -1.851);
    close(data.idle_air_control_duty_pct, 20.097);
    close(data.intake_camshaft_angle_deg, 60.375);
    close(data.exhaust_camshaft_angle_deg, -56.25);
    close(data.terminal_15_voltage_v, 13.2028);
    close(data.lambda_integrator_bank_1, 32768.0 * 0.000015259 + 0.5);
    close(data.lambda_integrator_bank_2, 16384.0 * 0.000015259 + 0.5);
    close(data.oxygen_sensor_heater_upstream_bank_1_duty_pct, 3.91);
    close(data.oxygen_sensor_heater_upstream_bank_2_duty_pct, 7.82);
    close(data.oxygen_sensor_heater_downstream_bank_1_duty_pct, 11.73);
    close(data.oxygen_sensor_heater_downstream_bank_2_duty_pct, 15.64);
    close(data.air_mass_per_stroke_mg, 212.0);
    close(data.knock_signal_cylinder_2_v, 0.0778);
    close(data.knock_signal_cylinder_5_v, 0.1556);
    close(data.cooling_fan_duty_pct, 50.00064);
    close(data.ambient_pressure_hpa, 1002.00528);
    close(data.battery_voltage_v, 13.7106);
    close(data.intake_vanos.actual_deg, 105.0);
    close(data.intake_vanos.target_deg, 108.0);
    close(
        data.intake_vanos.reference_deg,
        0x1234 as f64 * 0.001465 + 60.0,
    );
    close(data.exhaust_vanos.actual_deg, -90.0);
    close(data.exhaust_vanos.target_deg, -93.0);
    close(
        data.exhaust_vanos.reference_deg,
        0x2345 as f64 * -0.001465 - 60.0,
    );
    assert!(data.disa_command_active);
}

#[test]
fn short_final_block_is_an_error_instead_of_partial_or_fabricated_values() {
    let mut session = session(vec![
        (3, frame(&[0; 41])),
        (0x90, frame(&[0; 12])),
        (4, frame(&[0; 3])),
    ]);
    assert!(
        matches!(session.read_live_data(), Err(Error::LiveDataRead { block: 4, source, .. })
        if matches!(*source, Error::InvalidLength(7)))
    );
}

// Essential selection contract: no unrequested command or fabricated block.
#[test]
fn selected_reads_send_exactly_the_requested_blocks() {
    for mask in 1..8 {
        let selection = LiveDataSelection {
            measurements: mask & 1 != 0,
            vanos: mask & 2 != 0,
            digital: mask & 4 != 0,
            pedal: false,
            rough_running: false,
        };
        let mut exchanges = Vec::new();
        if selection.measurements {
            let mut measurements = [0; 41];
            measurements[..2].copy_from_slice(&1234_u16.to_be_bytes());
            exchanges.push((3, frame(&measurements)));
        }
        if selection.vanos {
            exchanges.push((0x90, frame(&[0; 12])));
        }
        if selection.digital {
            exchanges.push((4, frame(&[0, 0, 0, 4, 0, 0])));
        }
        let data = session(exchanges)
            .read_live_data_selected(selection)
            .unwrap();
        assert_eq!(data.measurements.is_some(), selection.measurements);
        assert_eq!(data.vanos.is_some(), selection.vanos);
        assert_eq!(data.digital.is_some(), selection.digital);
        if let Some(m) = data.measurements {
            assert_eq!(m.engine_speed_rpm, 1234);
        }
        if let Some(v) = data.vanos {
            close(v.intake.target_deg, 60.0);
        }
        if let Some(d) = data.digital {
            assert!(d.disa_command_active);
        }
    }
}

#[test]
fn empty_selection_is_rejected_before_identification_for_reads_and_streams() {
    let selection = LiveDataSelection {
        measurements: false,
        vanos: false,
        digital: false,
        pedal: false,
        rough_running: false,
    };
    let transport = ScriptedTransport {
        exchanges: VecDeque::new(),
        incoming: VecDeque::new(),
    };
    let mut session = Session::new(transport, Duration::from_secs(1)).unwrap();
    assert!(matches!(
        session.read_live_data_selected(selection),
        Err(Error::EmptyLiveDataSelection)
    ));
    assert!(matches!(
        session.stream_live_data_selected(selection, &|| false, &mut |_| {
            panic!("invalid selection must not emit events");
        }),
        Err(Error::EmptyLiveDataSelection)
    ));
}
