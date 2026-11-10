#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum DmeError {
    #[error("another operation is already using this client")]
    Busy,
    #[error("connection speed is uncertain; reset ECU/adapter to 9600 and create a new client")]
    NeedsReconnect,
    #[error("invalid argument: {details}")]
    InvalidArgument { details: String },
    #[error("transport: {details}")]
    Transport { details: String },
    #[error("protocol: {details}")]
    Protocol { details: String },
    #[error("DTC clear was not confirmed: {details}")]
    DtcClear { details: String },
    #[error("live data block {command:02X} {block:02X}: {details}")]
    LiveDataRead {
        command: u8,
        block: u8,
        details: String,
    },
    #[error("{operation}: {details}")]
    DiagnosticControl {
        operation: String,
        status: Option<u8>,
        details: String,
    },
    #[error("DTC page at index {start_index}: {details}")]
    DtcRead { start_index: u8, details: String },
    #[error("invalid image: {details}")]
    InvalidImage { details: String },
    #[error("ECU voltage is too low: {millivolts} mV")]
    LowVoltage { millivolts: u32 },
    #[error("image firmware {image} differs from ECU {ecu}")]
    FirmwareMismatch { image: String, ecu: String },
    #[error("flash: {details}")]
    Flash { details: String },
    #[error("{operation}; additionally, restoring 9600 baud failed: {restore}")]
    OperationAndRestore { operation: String, restore: String },
}

impl From<dmelab_ms43::Error> for DmeError {
    fn from(error: dmelab_ms43::Error) -> Self {
        use dmelab_ms43::Error;
        match error {
            Error::DtcClear { source } => Self::DtcClear {
                details: source.to_string(),
            },
            Error::LiveDataRead {
                command,
                block,
                source,
            } => Self::LiveDataRead {
                command,
                block,
                details: source.to_string(),
            },
            Error::DiagnosticControl { operation, source } => Self::DiagnosticControl {
                operation: operation.into(),
                status: match source.as_ref() {
                    Error::DiagnosticFunctionStatus(status) | Error::Rejected(status) => {
                        Some(*status)
                    }
                    _ => None,
                },
                details: source.to_string(),
            },
            Error::DtcRead {
                start_index,
                source,
            } => Self::DtcRead {
                start_index,
                details: source.to_string(),
            },
            Error::InvalidImage(details) => Self::InvalidImage { details },
            Error::Flash(details) => Self::Flash { details },
            Error::LowVoltage { millivolts } => Self::LowVoltage { millivolts },
            Error::FirmwareMismatch { image, ecu } => Self::FirmwareMismatch { image, ecu },
            Error::OperationAndRestore { operation, restore } => Self::OperationAndRestore {
                operation: operation.to_string(),
                restore: restore.to_string(),
            },
            Error::InvalidTimeout
            | Error::InvalidBaudRate(_)
            | Error::LiveDataBaudRate(_)
            | Error::EmptyLiveDataSelection
            | Error::InvalidCoolingFanDuty
            | Error::DiagnosticControlBaudRate(_) => Self::InvalidArgument {
                details: error.to_string(),
            },
            Error::Unsynchronized => Self::NeedsReconnect,
            Error::Io { .. }
            | Error::Timeout { .. }
            | Error::TransportSetup { .. }
            | Error::BaudRateConfiguration { .. } => Self::Transport {
                details: error.to_string(),
            },
            _ => Self::Protocol {
                details: error.to_string(),
            },
        }
    }
}

#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum TransportFailure {
    #[error("timeout: {details}")]
    Timeout { details: String },
    #[error("disconnected: {details}")]
    Disconnected { details: String },
    #[error("transport failure: {details}")]
    Other { details: String },
}

impl From<uniffi::UnexpectedUniFFICallbackError> for TransportFailure {
    fn from(error: uniffi::UnexpectedUniFFICallbackError) -> Self {
        Self::Other {
            details: error.to_string(),
        }
    }
}

impl From<TransportFailure> for std::io::Error {
    fn from(error: TransportFailure) -> Self {
        let kind = match &error {
            TransportFailure::Timeout { .. } => std::io::ErrorKind::TimedOut,
            TransportFailure::Disconnected { .. } => std::io::ErrorKind::NotConnected,
            TransportFailure::Other { .. } => std::io::ErrorKind::Other,
        };
        Self::new(kind, error)
    }
}
