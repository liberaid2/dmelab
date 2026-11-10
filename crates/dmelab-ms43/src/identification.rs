use crate::Error;

/// Identifiers read from the ECU. These are not a guarantee of flash compatibility.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Identification {
    /// Eight-byte identifier read through the descriptor's hardware pointer.
    pub hardware_id: String,
    /// Eight-byte signature at ECU address 0x0EFFCC; not a firmware version.
    pub flash_identifier: String,
    pub firmware_version: String,
}

pub(crate) fn text(bytes: &[u8], field: &'static str) -> Result<String, Error> {
    if !bytes.iter().all(|b| b.is_ascii_graphic() || *b == b' ') {
        return Err(Error::InvalidText { field });
    }
    String::from_utf8(bytes.to_vec()).map_err(|_| Error::InvalidText { field })
}
