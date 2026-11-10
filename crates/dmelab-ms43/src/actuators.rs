//! Explicit, one-shot diagnostic control. Never retry a possibly executed command.

use std::time::Duration;

use crate::session::LinkState;
use crate::{BaudRate, Error, Session};

/// Reject invalid input before identifying or configuring the adapter.
pub fn validate_cooling_fan_duty(duty_pct: f64) -> Result<(), Error> {
    if duty_pct.is_finite() && (0.0..=100.0).contains(&duty_pct) {
        Ok(())
    } else {
        Err(Error::InvalidCoolingFanDuty)
    }
}

impl Session {
    /// Set DISA actuation, not a measured flap position. No implicit end command.
    pub fn set_disa(&mut self, enabled: bool) -> Result<(), Error> {
        self.identify_for_control()?;
        self.control_exchange(&[0x22, 0x88, if enabled { 0xFF } else { 0 }])
            .and_then(check_function_status)
            .map_err(|source| control_error("DISA control", source))
    }

    /// Command fan PWM duty, not RPM. Use end_diagnostic_control to end the test.
    pub fn set_cooling_fan(&mut self, duty_pct: f64) -> Result<(), Error> {
        validate_cooling_fan_duty(duty_pct)?;
        self.identify_for_control()?;
        // MS430DS0 ECF_PWM_ECF FACT_A and flt2fix truncation. In particular,
        // 50.0 maps to 0x7F; keep the rounded PRG coefficient, not 100/256.
        let raw = (duty_pct / 0.39063) as u8;
        self.control_exchange(&[0x22, 0x8E, raw])
            .and_then(check_function_status)
            .map_err(|source| control_error("cooling fan control", source))
    }

    /// End diagnostic control using 9F, requiring A0. No retries or speed change.
    /// We never adjust fuel-consumption calibration, so PRG's optional verbrauch
    /// restore branch does not apply. This cannot restore another tool's state.
    pub fn end_diagnostic_control(&mut self) -> Result<(), Error> {
        self.identify_for_control()?;
        self.control_exchange(&[0x9F])
            .map(|_| ())
            .map_err(|source| control_error("end diagnostic control", source))
    }

    fn identify_for_control(&mut self) -> Result<(), Error> {
        match self.state {
            LinkState::Unidentified => {
                self.identify()?;
            }
            LinkState::Ready(_) => {}
            LinkState::Unsynchronized => return Err(Error::Unsynchronized),
        }
        match self.baud_rate() {
            Some(BaudRate::B9600) => Ok(()),
            Some(rate) => Err(Error::DiagnosticControlBaudRate(rate.as_u32())),
            None => Err(Error::Unsynchronized),
        }
    }

    fn control_exchange(&mut self, payload: &[u8]) -> Result<Vec<u8>, Error> {
        self.protocol
            .wait_after_exchange(Duration::from_millis(100));
        self.protocol.prepare()?;
        self.protocol.request(payload)
    }
}

fn check_function_status(response: Vec<u8>) -> Result<(), Error> {
    // Bare A0 is a valid acknowledgement. If ECU supplies a function status,
    // require zero rather than accepting an activation-condition error as success.
    if response.len() > 4 && response[3] != 0 {
        return Err(Error::DiagnosticFunctionStatus(response[3]));
    }
    Ok(())
}

fn control_error(operation: &'static str, source: Error) -> Error {
    Error::DiagnosticControl {
        operation,
        source: Box::new(source),
    }
}
