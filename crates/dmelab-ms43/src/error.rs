use std::io;

#[derive(Debug, thiserror::Error)]
pub enum Error {
    #[error("cooling fan duty must be a finite percentage from 0 to 100")]
    InvalidCoolingFanDuty,
    #[error("diagnostic control requires 9600 baud; current speed is {0}")]
    DiagnosticControlBaudRate(u32),
    #[error("{operation} was not confirmed: {source}")]
    DiagnosticControl {
        operation: &'static str,
        #[source]
        source: Box<Error>,
    },
    #[error("ECU diagnostic function returned status 0x{0:02X}")]
    DiagnosticFunctionStatus(u8),
    #[error("select at least one live data block")]
    EmptyLiveDataSelection,
    #[error("complete live data requires measurements, VANOS and digital blocks")]
    IncompleteLiveData,
    #[error("DTC clear was not confirmed: {source}")]
    DtcClear {
        #[source]
        source: Box<Error>,
    },
    #[error("failed to read live data block {command:02X} {block:02X}: {source}")]
    LiveDataRead {
        command: u8,
        block: u8,
        #[source]
        source: Box<Error>,
    },
    #[error("failed to read DTC page starting at record {start_index}: {source}")]
    DtcRead {
        start_index: u8,
        #[source]
        source: Box<Error>,
    },
    #[error("invalid or incomplete MS43 DTC page: {0}")]
    InvalidDtcPage(String),
    #[error("invalid MS43 image: {0}")]
    InvalidImage(String),
    #[error("flash operation failed: {0}")]
    Flash(String),
    #[error("ECU voltage is too low: {millivolts} mV; minimum is 11500 mV")]
    LowVoltage { millivolts: u32 },
    #[error("calibration firmware {image} does not match ECU firmware {ecu}")]
    FirmwareMismatch { image: String, ecu: String },
    #[error("{operation}; additionally, restoring 9600 baud failed: {restore}")]
    OperationAndRestore {
        #[source]
        operation: Box<Error>,
        restore: Box<Error>,
    },
    #[error("unsupported baud rate {0}; choose 9600, 19200, 38400, 50000, 107000, or 125000")]
    InvalidBaudRate(String),
    #[error("continuous live data requires 9600 baud; current speed is {0}")]
    LiveDataBaudRate(u32),
    #[error("identify the MS43 ECU before changing baud rate")]
    NotIdentified,
    #[error(
        "ECU/UART speed is uncertain after a failed switch; further communication is blocked; reestablish the connection with ECU and adapter at 9600 baud"
    )]
    Unsynchronized,
    #[error(
        "ECU accepted {baud_rate} baud, but configuring the local UART failed; ECU/UART speed is uncertain: {source}"
    )]
    BaudRateConfiguration {
        baud_rate: u32,
        #[source]
        source: io::Error,
    },
    #[error("timeout must be between 1 ms and 60 s")]
    InvalidTimeout,
    #[error("{stage}: communication timed out")]
    Timeout { stage: &'static str },
    #[error("transport setup ({stage}): {source}")]
    TransportSetup {
        stage: &'static str,
        #[source]
        source: io::Error,
    },
    #[error("{stage}: {source}")]
    Io {
        stage: &'static str,
        #[source]
        source: io::Error,
    },
    #[error("K-line echo does not match the request")]
    EchoMismatch,
    #[error("invalid response length: {0}")]
    InvalidLength(usize),
    #[error("unexpected response address: 0x{0:02X}")]
    InvalidAddress(u8),
    #[error("response XOR checksum is invalid")]
    InvalidChecksum,
    #[error("ECU rejected request with status 0x{0:02X}")]
    Rejected(u8),
    #[error("{field} is not printable ASCII")]
    InvalidText { field: &'static str },
    #[error("unsupported ECU hardware ID: {0}; expected MS43 (111430)")]
    UnsupportedHardware(String),
}

impl Error {
    pub(crate) fn io(stage: &'static str, source: io::Error) -> Self {
        if matches!(
            source.kind(),
            io::ErrorKind::TimedOut | io::ErrorKind::WouldBlock
        ) {
            Self::Timeout { stage }
        } else {
            Self::Io { stage, source }
        }
    }
}
