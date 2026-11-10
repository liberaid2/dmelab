use std::time::Duration;

use crate::live_extended::{decode_pedal, decode_rough_running};
use crate::session::LinkState;
use crate::{
    Error, LiveData, LiveDataSelection, LiveDigital, LiveMeasurements, LiveVanos, SelectedLiveData,
    Session, VanosPosition,
};

#[cfg(test)]
mod tests;

impl Session {
    /// Identify a fresh session, then read 0B 03, 0B 90 and 0B 04 once each.
    /// Uses the current baud rate, without actuator control or background work.
    /// An error in any block aborts the read; no partial LiveData is returned.
    pub fn read_live_data(&mut self) -> Result<LiveData, Error> {
        self.read_live_data_selected(LiveDataSelection::default())?
            .try_into()
    }

    /// Read any nonempty combination. An error discards the entire selected set.
    /// Unselected blocks generate no requests and appear as None in the result.
    pub fn read_live_data_selected(
        &mut self,
        selection: LiveDataSelection,
    ) -> Result<SelectedLiveData, Error> {
        selection.validate()?;
        match self.state {
            LinkState::Unidentified => {
                self.identify()?;
            }
            LinkState::Ready(_) => {}
            LinkState::Unsynchronized => return Err(Error::Unsynchronized),
        }
        // This single-read path cannot be cancelled; streams use the same reader
        // with a stop check between every selected block.
        self.read_selected_sample(selection, &|| false)?
            .ok_or(Error::IncompleteLiveData)
    }

    pub(super) fn read_selected_sample(
        &mut self,
        selection: LiveDataSelection,
        should_stop: &dyn Fn() -> bool,
    ) -> Result<Option<SelectedLiveData>, Error> {
        let mut data = SelectedLiveData {
            measurements: None,
            vanos: None,
            digital: None,
            pedal: None,
            rough_running: None,
        };
        if should_stop() {
            return Ok(None);
        }
        if selection.measurements {
            data.measurements = Some(decode_measurements(&self.read_live_block(0x0B, 0x03, 45)?));
        }
        if should_stop() {
            return Ok(None);
        }
        if selection.vanos {
            data.vanos = Some(decode_vanos(&self.read_live_block(0x0B, 0x90, 16)?));
        }
        if should_stop() {
            return Ok(None);
        }
        if selection.digital {
            let response = self.read_live_block(0x0B, 0x04, 10)?;
            // STATUS_DIGITAL: payload byte 3, mask/value 0x04.
            data.digital = Some(LiveDigital {
                disa_command_active: response[6] & 0x04 != 0,
            });
        }
        if should_stop() {
            return Ok(None);
        }
        if selection.pedal {
            data.pedal = Some(decode_pedal(&self.read_live_block(0x0B, 0x92, 30)?));
        }
        if should_stop() {
            return Ok(None);
        }
        if selection.rough_running {
            data.rough_running = Some(decode_rough_running(&self.read_live_block(0x2B, 0x05, 29)?));
        }
        Ok(Some(data))
    }

    pub(super) fn read_live_block(
        &mut self,
        command: u8,
        block: u8,
        min_length: usize,
    ) -> Result<Vec<u8>, Error> {
        let result = (|| {
            // INITIALISIERUNG in MS430DS0: ParRegenTime = 100 ms. Keep this
            // local to live reads; flash has its own established timings.
            self.protocol
                .wait_after_exchange(Duration::from_millis(100));
            self.protocol.prepare()?;
            let response = self.protocol.request(&[command, block])?;
            if response.len() < min_length {
                return Err(Error::InvalidLength(response.len()));
            }
            Ok(response)
        })();
        result.map_err(|source| Error::LiveDataRead {
            command,
            block,
            source: Box::new(source),
        })
    }
}

// MS430DS0.b1v BETRIEBSWTAB: offsets are in complete DS2 responses (header
// included). DATA_TYPE 5 is big-endian u16, 7 is big-endian i16, 3 is i8.
// Signed conversions follow the table-driven PRG decoder. Lengths checked above.
fn word(bytes: &[u8], offset: usize) -> u16 {
    u16::from_be_bytes([bytes[offset], bytes[offset + 1]])
}

fn decode_measurements(m: &[u8]) -> LiveMeasurements {
    LiveMeasurements {
        engine_speed_rpm: word(m, 3),
        vehicle_speed_kmh: m[5],
        accelerator_pedal_angle_deg: f64::from(word(m, 6)) * 0.0018311,
        throttle_angle_deg: f64::from(word(m, 8)) * 0.0018311,
        mass_airflow_kgh: f64::from(word(m, 10)) * 0.25,
        intake_air_temperature_c: f64::from(m[12]) * 0.75 - 48.0,
        coolant_temperature_c: f64::from(m[13]) * 0.75 - 48.0,
        oil_temperature_c: f64::from(m[14]) * 0.796 - 48.0,
        radiator_outlet_temperature_c: f64::from(m[15]) * 0.75 - 48.0,
        ignition_timing_cylinder_1_deg: f64::from(m[16]) * -0.375 + 72.0,
        injection_time_bank_1_ms: f64::from(word(m, 17)) * 0.0053333,
        idle_speed_integrator_pct: f64::from(i16::from_be_bytes([m[19], m[20]])) * 0.0015,
        idle_air_control_duty_pct: f64::from(word(m, 21)) * 0.0015,
        intake_camshaft_angle_deg: f64::from(m[23]) * 0.375 + 60.0,
        exhaust_camshaft_angle_deg: f64::from(m[24] as i8) * -0.375 - 60.0,
        terminal_15_voltage_v: f64::from(m[25]) * 0.10156,
        lambda_integrator_bank_1: f64::from(word(m, 26)) * 0.000015259 + 0.5,
        lambda_integrator_bank_2: f64::from(word(m, 28)) * 0.000015259 + 0.5,
        oxygen_sensor_heater_upstream_bank_1_duty_pct: f64::from(m[30]) * 0.391,
        oxygen_sensor_heater_upstream_bank_2_duty_pct: f64::from(m[31]) * 0.391,
        oxygen_sensor_heater_downstream_bank_1_duty_pct: f64::from(m[32]) * 0.391,
        oxygen_sensor_heater_downstream_bank_2_duty_pct: f64::from(m[33]) * 0.391,
        air_mass_per_stroke_mg: f64::from(word(m, 34)) * 0.0212,
        knock_signal_cylinder_2_v: f64::from(word(m, 36)) * 0.0000778,
        knock_signal_cylinder_5_v: f64::from(word(m, 38)) * 0.0000778,
        cooling_fan_duty_pct: f64::from(m[40]) * 0.39063,
        ambient_pressure_hpa: f64::from(word(m, 41)) * 0.08292,
        battery_voltage_v: f64::from(m[43]) * 0.10156,
    }
}

fn decode_vanos(v: &[u8]) -> LiveVanos {
    LiveVanos {
        intake: VanosPosition {
            actual_deg: f64::from(v[3]) * 0.375 + 60.0,
            target_deg: f64::from(v[4]) * 0.375 + 60.0,
            reference_deg: f64::from(word(v, 5)) * 0.001465 + 60.0,
        },
        exhaust: VanosPosition {
            actual_deg: f64::from(v[9]) * -0.375 - 60.0,
            target_deg: f64::from(v[10]) * -0.375 - 60.0,
            reference_deg: f64::from(word(v, 11)) * -0.001465 - 60.0,
        },
    }
}
