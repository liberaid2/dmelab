use std::str::FromStr;

use crate::Error;

/// UART speeds offered by the researched MS4x Flasher MS43 implementation.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum BaudRate {
    B9600,
    B19200,
    B38400,
    B50000,
    B107000,
    B125000,
}

impl BaudRate {
    pub fn as_u32(self) -> u32 {
        match self {
            Self::B9600 => 9600,
            Self::B19200 => 19200,
            Self::B38400 => 38400,
            Self::B50000 => 50000,
            Self::B107000 => 107000,
            Self::B125000 => 125000,
        }
    }

    pub(crate) fn payload(self) -> [u8; 5] {
        // Preserve the original mismatch: the 107000 UART mode requests
        // 100000 from the ECU. Do not derive this payload from as_u32().
        let ecu_rate: u32 = match self {
            Self::B107000 => 100000,
            other => other.as_u32(),
        };
        let bytes = ecu_rate.to_be_bytes();
        [0x91, bytes[1], bytes[2], bytes[3], 0x00]
    }
}

impl FromStr for BaudRate {
    type Err = Error;

    fn from_str(value: &str) -> Result<Self, Self::Err> {
        match value {
            "9600" => Ok(Self::B9600),
            "19200" => Ok(Self::B19200),
            "38400" => Ok(Self::B38400),
            "50000" => Ok(Self::B50000),
            "107000" => Ok(Self::B107000),
            "125000" => Ok(Self::B125000),
            _ => Err(Error::InvalidBaudRate(value.to_owned())),
        }
    }
}
