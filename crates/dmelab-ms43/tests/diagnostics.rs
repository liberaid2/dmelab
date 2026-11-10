//! Essential wire contracts for new read blocks and state-changing commands.
use std::collections::VecDeque;
use std::io::{self, Read, Write};
use std::time::Duration;

use dmelab_core::Transport;
use dmelab_ms43::{Error, LiveDataSelection, Session};

type Exchange = (Vec<u8>, Result<Vec<u8>, io::ErrorKind>);
struct Wire {
    exchanges: VecDeque<Exchange>,
    incoming: VecDeque<u8>,
    failure: Option<io::ErrorKind>,
}
impl Transport for Wire {
    fn set_timeout(&mut self, timeout: Duration) -> io::Result<()> {
        assert!(!timeout.is_zero());
        Ok(())
    }
    fn set_baud_rate(&mut self, _: u32) -> io::Result<()> {
        panic!("unexpected speed change");
    }
    fn clear_buffers(&mut self) -> io::Result<()> {
        self.incoming.clear();
        Ok(())
    }
}
impl Write for Wire {
    fn write(&mut self, bytes: &[u8]) -> io::Result<usize> {
        let (payload, response) = self
            .exchanges
            .pop_front()
            .expect("unexpected command/retry");
        let mut expected = vec![0x12, (payload.len() + 3) as u8];
        expected.extend(payload);
        expected.push(expected.iter().fold(0, |a, b| a ^ b));
        assert_eq!(bytes, expected);
        self.incoming.extend(bytes);
        match response {
            Ok(bytes) => self.incoming.extend(bytes),
            Err(kind) => self.failure = Some(kind),
        }
        Ok(bytes.len())
    }
    fn flush(&mut self) -> io::Result<()> {
        Ok(())
    }
}
impl Read for Wire {
    fn read(&mut self, bytes: &mut [u8]) -> io::Result<usize> {
        if self.incoming.is_empty() {
            return Err(self
                .failure
                .take()
                .unwrap_or(io::ErrorKind::TimedOut)
                .into());
        }
        let count = bytes.len().min(3).min(self.incoming.len());
        for byte in &mut bytes[..count] {
            *byte = self.incoming.pop_front().unwrap();
        }
        Ok(count)
    }
}
impl Drop for Wire {
    fn drop(&mut self) {
        if !std::thread::panicking() {
            assert!(self.exchanges.is_empty(), "missing command");
        }
    }
}
fn response(status: u8, data: &[u8]) -> Vec<u8> {
    let mut frame = vec![0x12, (data.len() + 4) as u8, status];
    frame.extend(data);
    frame.push(frame.iter().fold(0, |a, b| a ^ b));
    frame
}
fn exchange(payload: &[u8], data: &[u8]) -> Exchange {
    (payload.to_vec(), Ok(response(0xA0, data)))
}
fn wire(exchanges: Vec<Exchange>) -> Wire {
    Wire {
        exchanges: exchanges.into(),
        incoming: VecDeque::new(),
        failure: None,
    }
}
fn session(mut exchanges: Vec<Exchange>) -> Session {
    let mut descriptor = [0; 57];
    descriptor[54..].copy_from_slice(&[0x12, 0x34, 0x56]);
    let mut identification = vec![
        exchange(&[0x0D], &descriptor),
        exchange(&[6, 0, 0x12, 0x34, 0x56, 8], b"11143000"),
        exchange(&[6, 0, 0x0E, 0xFF, 0xCC, 8], b"11143000"),
        exchange(&[6, 0, 0x0E, 0xFF, 0xBA, 6], b"430069"),
    ];
    identification.append(&mut exchanges);
    Session::new(wire(identification), Duration::from_millis(100)).unwrap()
}
fn selected() -> LiveDataSelection {
    LiveDataSelection {
        measurements: false,
        vanos: false,
        digital: false,
        pedal: true,
        rough_running: true,
    }
}
fn close(actual: f64, expected: f64) {
    assert!((actual - expected).abs() < 1e-9, "{actual} != {expected}");
}

#[test]
fn new_blocks_decode_signed_adaptations_all_channels_and_wire_cylinder_order() {
    let words = [
        -1234_i16, 2345, -3456, 4000, 5000, 6000, 7000, 101, 202, 303, 404, 505, 606,
    ];
    let pedal: Vec<u8> = words.into_iter().flat_map(i16::to_be_bytes).collect();
    let mut rough = vec![6];
    for value in [-100_i16, -500, -300, 600, 200, 400] {
        rough.extend(value.to_be_bytes());
    }
    for value in [11_u16, 55, 33, 66, 22, 44] {
        rough.extend(value.to_be_bytes());
    }
    let mut session = session(vec![
        exchange(&[0x0B, 0x92], &pedal),
        exchange(&[0x2B, 5], &rough),
    ]);
    let data = session.read_live_data_selected(selected()).unwrap();
    assert!(data.measurements.is_none() && data.vanos.is_none() && data.digital.is_none());
    let p = data.pedal.unwrap();
    close(p.idle_adaptation_pct, -1234.0 * 0.001526);
    close(p.idle_adaptation_in_gear_pct, 2345.0 * 0.001526);
    close(p.idle_adaptation_ac_pct, -3456.0 * 0.001526);
    close(p.accelerator_pedal_sensor_1_angle_deg, 4000.0 * 0.0018311);
    close(p.accelerator_pedal_sensor_2_angle_deg, 5000.0 * 0.0018311);
    close(p.throttle_sensor_1_angle_deg, 6000.0 * 0.007294);
    close(p.throttle_sensor_2_angle_deg, 7000.0 * 0.007294);
    for (actual, raw) in [
        p.throttle_mechanical_stop_sensor_1_voltage_v,
        p.throttle_mechanical_stop_sensor_2_voltage_v,
        p.throttle_electrical_stop_sensor_1_voltage_v,
        p.throttle_electrical_stop_sensor_2_voltage_v,
        p.throttle_limp_home_sensor_1_voltage_v,
        p.throttle_limp_home_sensor_2_voltage_v,
    ]
    .into_iter()
    .zip([101, 202, 303, 404, 505, 606])
    {
        close(actual, f64::from(raw) * 0.0048828);
    }
    let r = data.rough_running.unwrap();
    assert_eq!(r.status, 6);
    for (index, cylinder) in r.cylinders.iter().enumerate() {
        let number = (index + 1) as u8;
        assert_eq!(cylinder.cylinder, number);
        close(
            cylinder.roughness,
            f64::from(number) * if number % 2 == 1 { -100.0 } else { 100.0 } * 0.00122,
        );
        assert_eq!(cylinder.misfire_count, u16::from(number) * 11);
    }
    assert_eq!(r.cylinders.len(), 6);
}

#[test]
fn diagnostic_commands_require_acknowledgement_and_never_retry_or_write_calibration() {
    let mut session = session(vec![
        exchange(&[0x22, 0x88, 0xFF], &[]),
        exchange(&[0x22, 0x88, 0], &[0]),
        exchange(&[0x22, 0x8E, 0x7F], &[]),
        exchange(&[0x22, 0x8E, 0xFF], &[0]),
        exchange(&[0x9F], &[]),
        exchange(&[0x22, 0x88, 0xFF], &[1]),
        (vec![0x22, 0x8E, 0], Err(io::ErrorKind::TimedOut)),
        (vec![0x9F], Ok(response(0xB1, &[]))),
    ]);
    session.set_disa(true).unwrap();
    session.set_disa(false).unwrap();
    session.set_cooling_fan(50.0).unwrap();
    session.set_cooling_fan(100.0).unwrap();
    session.end_diagnostic_control().unwrap();
    assert!(
        matches!(session.set_disa(true), Err(Error::DiagnosticControl { source, .. }) if matches!(*source, Error::DiagnosticFunctionStatus(1)))
    );
    assert!(
        matches!(session.set_cooling_fan(0.0), Err(Error::DiagnosticControl { source, .. }) if matches!(*source, Error::Timeout { .. }))
    );
    assert!(
        matches!(session.end_diagnostic_control(), Err(Error::DiagnosticControl { source, .. }) if matches!(*source, Error::Rejected(0xB1)))
    );
}

#[test]
fn invalid_fan_duty_never_touches_an_unidentified_ecu() {
    let mut session = Session::new(wire(vec![]), Duration::from_millis(100)).unwrap();
    for duty in [f64::NAN, f64::INFINITY, -1.0, 100.01] {
        assert!(matches!(
            session.set_cooling_fan(duty),
            Err(Error::InvalidCoolingFanDuty)
        ));
    }
}

#[test]
fn selected_stream_restarts_new_blocks_after_a_short_rough_running_reply() {
    use dmelab_ms43::SelectedLiveDataEvent;
    use std::ops::ControlFlow;
    let mut session = session(vec![
        exchange(&[0x0B, 0x92], &[0; 26]),
        exchange(&[0x2B, 5], &[5]),
        exchange(&[0x0B, 0x92], &[0; 26]),
        exchange(&[0x2B, 5], &[0; 25]),
    ]);
    let mut failures = 0;
    let mut samples = 0;
    session
        .stream_live_data_selected(selected(), &|| false, &mut |event| match event {
            SelectedLiveDataEvent::Recovering { error, .. } => {
                assert!(matches!(
                    error,
                    Error::LiveDataRead {
                        command: 0x2B,
                        block: 5,
                        ..
                    }
                ));
                failures += 1;
                ControlFlow::Continue(())
            }
            SelectedLiveDataEvent::Sample(data) => {
                assert!(data.pedal.is_some() && data.rough_running.is_some());
                samples += 1;
                ControlFlow::Break(())
            }
        })
        .unwrap();
    assert_eq!((failures, samples), (1, 1));
}
