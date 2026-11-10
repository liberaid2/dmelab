use std::time::Duration;

use dmelab_core::Transport;

use crate::identification::text;
use crate::protocol::Protocol;
use crate::{BaudRate, Error, Identification};

#[derive(Clone, Copy)]
pub(crate) enum LinkState {
    Unidentified,
    Ready(BaudRate),
    Unsynchronized,
}

/// A synchronous, exclusively owned MS43 session. No threads are created.
pub struct Session {
    pub(crate) protocol: Protocol,
    pub(crate) state: LinkState,
}

impl Session {
    /// The caller must supply a transport initially configured to 9600 baud, 8E1.
    pub fn new(transport: impl Transport + 'static, timeout: Duration) -> Result<Self, Error> {
        Ok(Self {
            protocol: Protocol::new(transport, timeout)?,
            state: LinkState::Unidentified,
        })
    }

    /// Identify MS43 without unlocking, erasing, writing, or changing baud rate.
    pub fn identify(&mut self) -> Result<Identification, Error> {
        let baud_rate = match self.state {
            LinkState::Unidentified => BaudRate::B9600,
            LinkState::Ready(rate) => rate,
            LinkState::Unsynchronized => return Err(Error::Unsynchronized),
        };
        self.protocol.prepare()?;
        let descriptor = self.protocol.request(&[0x0D])?;
        // Full response offsets 57..60 contain a 24-bit hardware ID pointer.
        // Include the checksum byte when validating the minimum packet length.
        if descriptor.len() < 61 {
            return Err(Error::InvalidLength(descriptor.len()));
        }
        let address = u32::from_be_bytes([0, descriptor[57], descriptor[58], descriptor[59]]);
        // Observed gap in MS4x Flasher communication method 0x06000021.
        std::thread::sleep(Duration::from_millis(100));
        let hardware_id = text(&self.read_memory(address, 8)?, "hardware ID")?;
        if !hardware_id.starts_with("111430") {
            return Err(Error::UnsupportedHardware(hardware_id));
        }
        self.protocol.prepare()?;
        let flash_identifier = text(&self.read_memory(0x0EFFCC, 8)?, "flash identifier")?;
        self.protocol.prepare()?;
        let firmware_version = text(&self.read_memory(0x0EFFBA, 6)?, "firmware version")?;
        self.state = LinkState::Ready(baud_rate);
        Ok(Identification {
            hardware_id,
            flash_identifier,
            firmware_version,
        })
    }

    pub(crate) fn read_memory(&mut self, address: u32, length: u8) -> Result<Vec<u8>, Error> {
        let address = address.to_be_bytes();
        let payload = [0x06, address[0], address[1], address[2], address[3], length];
        let response = self.protocol.request(&payload)?;
        if response.len() != usize::from(length) + 4 {
            return Err(Error::InvalidLength(response.len()));
        }
        Ok(response[3..response.len() - 1].to_vec())
    }
}
