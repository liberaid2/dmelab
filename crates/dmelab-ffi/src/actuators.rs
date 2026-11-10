//! One-shot actuator commands; execution and USB ownership stay with the caller.

use crate::{DmeError, Ms43Client};

#[uniffi::export]
impl Ms43Client {
    pub fn set_disa(&self, enabled: bool, timeout_ms: u32) -> Result<(), DmeError> {
        self.execute(timeout_ms, |session| session.set_disa(enabled))
    }

    pub fn set_cooling_fan(&self, duty_pct: f64, timeout_ms: u32) -> Result<(), DmeError> {
        dmelab_ms43::validate_cooling_fan_duty(duty_pct)?;
        self.execute(timeout_ms, |session| session.set_cooling_fan(duty_pct))
    }

    pub fn end_diagnostic_control(&self, timeout_ms: u32) -> Result<(), DmeError> {
        self.execute(timeout_ms, |session| session.end_diagnostic_control())
    }
}
