//! Synchronous UniFFI boundary; apps own execution, and Android owns USB access.

mod actuators;
mod client;
mod error;
mod live_data;
mod live_extended;
mod live_selection;
mod live_stream;
mod model;
#[cfg(feature = "desktop")]
mod serial;
mod transport;

pub use client::{Ms43Client, ProgressListener};
pub use error::{DmeError, TransportFailure};
pub use live_data::{LiveData, VanosPosition};
pub use live_extended::{CylinderRunning, LivePedal, LiveRoughRunning};
pub use live_selection::{
    LiveDataSelection, LiveDigital, LiveMeasurements, LiveVanos, SelectedLiveData,
};
pub use live_stream::{
    LiveDataListener, LiveDataStreamControl, LiveDataStreamFailure, SelectedLiveDataListener,
};
pub use model::{BaudRate, Dtc, FlashMode, Identification, Progress};
#[cfg(feature = "desktop")]
pub use serial::{SerialPortInfo, list_serial_ports};
pub use transport::KlineTransport;

uniffi::setup_scaffolding!();

#[uniffi::export]
pub fn library_version() -> String {
    env!("CARGO_PKG_VERSION").to_owned()
}

#[uniffi::export]
pub fn validate_image(image: Vec<u8>, mode: FlashMode) -> Result<(), DmeError> {
    dmelab_ms43::validate_image(&image, mode.into()).map_err(Into::into)
}
