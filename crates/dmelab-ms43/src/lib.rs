//! Synchronous MS43 diagnostics and flash over an echoing K-line transport.

mod actuators;
mod baud_rate;
mod baud_switch;
mod dtc;
mod error;
mod flash;
mod identification;
mod live_data;
mod live_extended;
mod live_model;
mod live_stream;
mod protocol;
mod session;

pub use actuators::validate_cooling_fan_duty;
pub use baud_rate::BaudRate;
pub use dtc::Dtc;
pub use error::Error;
pub use flash::{FlashMode, FlashProgress, validate_image};
pub use identification::Identification;
pub use live_extended::{CylinderRunning, LivePedal, LiveRoughRunning};
pub use live_model::{
    LiveData, LiveDataSelection, LiveDigital, LiveMeasurements, LiveVanos, SelectedLiveData,
    VanosPosition,
};
pub use live_stream::{LiveDataEvent, SelectedLiveDataEvent};
pub use session::Session;
