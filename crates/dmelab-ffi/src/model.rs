#[derive(Debug, Clone, Copy, uniffi::Enum)]
pub enum BaudRate {
    B9600,
    B19200,
    B38400,
    B50000,
    B107000,
    B125000,
}

impl From<BaudRate> for dmelab_ms43::BaudRate {
    fn from(value: BaudRate) -> Self {
        match value {
            BaudRate::B9600 => Self::B9600,
            BaudRate::B19200 => Self::B19200,
            BaudRate::B38400 => Self::B38400,
            BaudRate::B50000 => Self::B50000,
            BaudRate::B107000 => Self::B107000,
            BaudRate::B125000 => Self::B125000,
        }
    }
}

#[derive(Debug, Clone, Copy, uniffi::Enum)]
pub enum FlashMode {
    Full,
    Calibration,
}

impl From<FlashMode> for dmelab_ms43::FlashMode {
    fn from(value: FlashMode) -> Self {
        match value {
            FlashMode::Full => Self::Full,
            FlashMode::Calibration => Self::Calibration,
        }
    }
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct Identification {
    pub hardware_id: String,
    pub flash_identifier: String,
    pub firmware_version: String,
}

impl From<dmelab_ms43::Identification> for Identification {
    fn from(value: dmelab_ms43::Identification) -> Self {
        Self {
            hardware_id: value.hardware_id,
            flash_identifier: value.flash_identifier,
            firmware_version: value.firmware_version,
        }
    }
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct Dtc {
    pub code: u8,
    pub frequency: u8,
    pub data: Vec<u8>,
    pub raw: Vec<u8>,
}

impl From<dmelab_ms43::Dtc> for Dtc {
    fn from(value: dmelab_ms43::Dtc) -> Self {
        Self {
            code: value.code,
            frequency: value.frequency,
            data: value.data.to_vec(),
            raw: value.raw.to_vec(),
        }
    }
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct Progress {
    pub stage: String,
    pub completed: u64,
    pub total: u64,
}

impl From<dmelab_ms43::FlashProgress> for Progress {
    fn from(value: dmelab_ms43::FlashProgress) -> Self {
        Self {
            stage: value.stage.to_owned(),
            completed: value.completed as u64,
            total: value.total as u64,
        }
    }
}
