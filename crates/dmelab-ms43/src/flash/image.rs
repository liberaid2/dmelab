//! Offline MS43 image recognition; file offsets differ from ECU addresses.

use super::FlashMode;
use crate::Error;

pub(super) const FULL_SIZE: usize = 0x80000;
pub(super) const CALIBRATION_SIZE: usize = 0x10000;

/// Validate the original MS4x Flasher size and MS43 identifier checks.
pub fn validate_image(image: &[u8], mode: FlashMode) -> Result<(), Error> {
    let signature_offset = match image.len() {
        FULL_SIZE => 0x6ffcc,
        CALIBRATION_SIZE if mode == FlashMode::Calibration => 0x20,
        _ => {
            return Err(Error::InvalidImage(
                "expected a 512 KiB full image, or 64 KiB for calibration write".into(),
            ));
        }
    };
    if !image[signature_offset..signature_offset + 8]
        .windows(6)
        .any(|window| window == b"111430")
    {
        return Err(Error::InvalidImage(
            "MS43 identifier 111430 is missing".into(),
        ));
    }
    Ok(())
}

/// Read the six-byte software version used by the calibration safety check.
pub fn image_firmware_version(image: &[u8]) -> Result<String, Error> {
    let offset = match image.len() {
        FULL_SIZE => 0x6ffba,
        CALIBRATION_SIZE => 8,
        _ => return Err(Error::InvalidImage("unsupported image size".into())),
    };
    crate::identification::text(&image[offset..offset + 6], "image firmware version")
}
