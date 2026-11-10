//! Standard checksum branch of MS4x Flasher 1.6.0 (binary methods 0x22–0x28).
//! These are offline changes only: correction never authorizes bootloader writes.

use super::image::{CALIBRATION_SIZE, FULL_SIZE};
use crate::Error;

/// Correct CRC16 and monitor sums in the original correction order.
/// A calibration image requires the ECU's 40 bytes at 0x0efdb8 or a matching full
/// image as context; an incomplete correction is never silently accepted.
pub fn correct_checksums(image: &mut [u8], context: Option<&[u8]>) -> Result<(), Error> {
    if !matches!(image.len(), FULL_SIZE | CALIBRATION_SIZE) {
        return Err(invalid("unsupported checksum image size"));
    }
    let context = match context {
        Some(bytes) if bytes.len() == FULL_SIZE => Some(&bytes[0x6fdb8..0x6fde0]),
        Some(bytes) if bytes.len() == 40 => Some(bytes),
        Some(_) => {
            return Err(invalid(
                "monitor context must contain 40 bytes or a full image",
            ));
        }
        None => None,
    };
    if image.len() == FULL_SIZE {
        boot_crc(image)?;
        monitor(image, None, false)?;
        let seed = pointer(image, 0x6ed42)?;
        let initial = word(image, seed)?;
        section_crc(image, 0x6fde2, 0x6fde4, initial, 0x6fde0)?;
        monitor(image, None, true)?;
    } else {
        if context.is_none() {
            return Err(invalid(
                "calibration monitor correction requires ECU context",
            ));
        }
        monitor(image, context, true)?;
    }
    let seed = if image.len() == CALIBRATION_SIZE {
        0x0c
    } else {
        pointer(image, 0x6ed5a)?
    };
    let initial = word(image, seed)?;
    section_crc(image, 0x73fe2, 0x73fe4, initial, 0x73fe0)
}

fn boot_crc(image: &mut [u8]) -> Result<(), Error> {
    // Boot pointers are physical file offsets, unlike program page pointers.
    let start = usize::from(word(image, 0x3c28)?) | (usize::from(word(image, 0x3c2a)?) << 16);
    let end = usize::from(word(image, 0x3c2c)?) | (usize::from(word(image, 0x3c2e)?) << 16);
    let value = crc16(image, start, end, word(image, 0x3fe6)?)?;
    store(image, 0x3c24, value)
}

fn section_crc(
    image: &mut [u8],
    count_address: usize,
    table: usize,
    mut value: u16,
    output: usize,
) -> Result<(), Error> {
    let count = usize::from(word(image, count_address)?);
    if !(1..=0x1000).contains(&count) {
        return Err(invalid("invalid CRC range count"));
    }
    for index in 0..count {
        let start = pointer(image, table + 8 * index)?;
        let end = pointer(image, table + 8 * index + 4)?;
        value = crc16(image, start, end, value)?;
    }
    store(image, output, value)
}

fn crc16(image: &[u8], start: usize, end: usize, mut value: u16) -> Result<u16, Error> {
    if start > end || end >= image.len() {
        return Err(invalid(format!(
            "CRC range outside image: {start:#x}..{end:#x}"
        )));
    }
    for byte in &image[start..=end] {
        value ^= u16::from(*byte);
        for _ in 0..8 {
            value = (value >> 1) ^ if value & 1 != 0 { 0xa001 } else { 0 };
        }
    }
    Ok(value)
}

fn monitor(image: &mut [u8], context: Option<&[u8]>, calibration: bool) -> Result<(), Error> {
    let (table, seed, output) = if calibration {
        (0x6fdce, 0x6fdb8, 0x72ffc)
    } else {
        (0x6fdbe, 0x6fdb2, 0x6fdae)
    };
    let mut value = u32::from(monitor_word(image, context, seed)?)
        | (u32::from(monitor_word(image, context, seed + 2)?) << 16);
    for index in 0..2 {
        let entry = table + 8 * index;
        let start_low = monitor_word(image, context, entry)?;
        let start_high = monitor_word(image, context, entry + 2)?;
        let end_low = monitor_word(image, context, entry + 4)?;
        // The original reads the high end word but uses the low 16-bit
        // difference for the length, including wrapping across a page boundary.
        monitor_word(image, context, entry + 6)?;
        let count = usize::from(end_low.wrapping_sub(start_low) >> 1);
        let start = usize::from(start_low) | (usize::from(start_high) << 16);
        for offset in 0..count {
            let address = start
                .checked_add(offset * 2)
                .ok_or_else(|| invalid("monitor address overflow"))?;
            let mapped = map_page(address >> 16, image.len() == CALIBRATION_SIZE)? << 16
                | (address & 0xffff);
            value = value.wrapping_add(u32::from(word(image, mapped)?));
        }
    }
    store(image, output, value as u16)?;
    store(image, output + 2, (value >> 16) as u16)
}

fn monitor_word(image: &[u8], context: Option<&[u8]>, address: usize) -> Result<u16, Error> {
    if image.len() == FULL_SIZE {
        return word(image, address);
    }
    let metadata = context.ok_or_else(|| invalid("missing monitor context"))?;
    let offset = address
        .checked_sub(0x6fdb8)
        .ok_or_else(|| invalid("invalid metadata address"))?;
    read_word(metadata, offset)
}

fn pointer(image: &[u8], address: usize) -> Result<usize, Error> {
    let low = usize::from(word(image, address)?);
    let page = map_page(
        usize::from(word(image, address + 2)?),
        image.len() == CALIBRATION_SIZE,
    )?;
    Ok(low | (page << 16))
}

fn map_page(page: usize, calibration_only: bool) -> Result<usize, Error> {
    if calibration_only {
        // A calibration-only buffer cannot provide program/bootloader words.
        return match page {
            0 | 7 => Ok(0),
            _ => Err(invalid(format!(
                "page {page:#x} lies outside calibration image"
            ))),
        };
    }
    Ok(if (9..=14).contains(&page) {
        page - 8
    } else {
        page
    })
}

fn image_offset(image: &[u8], address: usize) -> Result<usize, Error> {
    if image.len() == CALIBRATION_SIZE {
        match address >> 16 {
            0 | 7 => Ok(address & 0xffff),
            _ => Err(invalid("word address lies outside calibration image")),
        }
    } else {
        Ok(address)
    }
}

fn word(image: &[u8], address: usize) -> Result<u16, Error> {
    read_word(image, image_offset(image, address)?)
}

fn read_word(bytes: &[u8], offset: usize) -> Result<u16, Error> {
    let pair = bytes
        .get(offset..offset.saturating_add(2))
        .filter(|pair| pair.len() == 2)
        .ok_or_else(|| invalid(format!("word outside image: {offset:#x}")))?;
    Ok(u16::from_le_bytes([pair[0], pair[1]]))
}

fn store(image: &mut [u8], address: usize, value: u16) -> Result<(), Error> {
    let offset = image_offset(image, address)?;
    let target = image
        .get_mut(offset..offset.saturating_add(2))
        .filter(|pair| pair.len() == 2)
        .ok_or_else(|| invalid(format!("checksum output outside image: {address:#x}")))?;
    target.copy_from_slice(&value.to_le_bytes());
    Ok(())
}

fn invalid(message: impl Into<String>) -> Error {
    Error::InvalidImage(message.into())
}

#[cfg(test)]
mod tests {
    use super::*;

    // Essential firmware integrity check: expected bytes independently confirmed
    // against ms43_checksums.py AND verify_ms43.py's original CIL interpreter.
    #[test]
    fn correction_matches_original_full_and_calibration_vectors() {
        assert_eq!(crc16(b"123456789", 0, 8, 0).unwrap(), 0xbb3d);
        let original = fixture();
        let mut expected = original.clone();
        for (offset, value) in [
            (0x3c24, 0x15e2_u16),
            (0x6fdae, 0x8a4d),
            (0x6fdb0, 0x001e),
            (0x6fde0, 0x07af),
            (0x72ffc, 0x17cb),
            (0x72ffe, 0x001d),
            (0x73fe0, 0x94ae),
        ] {
            expected[offset..offset + 2].copy_from_slice(&value.to_le_bytes());
        }
        let mut actual = original.clone();
        correct_checksums(&mut actual, None).unwrap();
        assert_eq!(actual, expected);
        let mut calibration = original[0x70000..].to_vec();
        assert!(correct_checksums(&mut calibration, None).is_err());
        assert_eq!(calibration, original[0x70000..]);
        correct_checksums(&mut calibration, Some(&original[0x6fdb8..0x6fde0])).unwrap();
        assert_eq!(calibration, expected[0x70000..]);
    }

    fn fixture() -> Vec<u8> {
        let mut image: Vec<u8> = (0..FULL_SIZE).map(|i| (i * 13 + 7) as u8).collect();
        image[0x6ffcc..0x6ffd4].copy_from_slice(b"111430xx");
        image[0x70020..0x70028].copy_from_slice(b"111430xx");
        image[0x6ffba..0x6ffc0].copy_from_slice(b"430069");
        image[0x70008..0x7000e].copy_from_slice(b"430069");
        for (offset, address) in [
            (0x3c28, 0x200_usize),
            (0x3c2c, 0x2ff),
            (0x6ed42, 0x90100),
            (0x6fde4, 0x6fdae),
            (0x6fde8, 0x6fdbb),
            (0x6fdec, 0x90100),
            (0x6fdf0, 0x9013f),
            (0x6ed5a, 0x7000c),
            (0x73fe4, 0x70000),
            (0x73fe8, 0x7007f),
            (0x73fec, 0x72ffc),
            (0x73ff0, 0x72fff),
            (0x6fdbe, 0x9fff0),
            (0x6fdc2, 0xa0030),
            (0x6fdc6, 0xe1000),
            (0x6fdca, 0xe1040),
            (0x6fdce, 0x70020),
            (0x6fdd2, 0x70060),
            (0x6fdd6, 0x70100),
            (0x6fdda, 0x70140),
        ] {
            image[offset..offset + 4].copy_from_slice(&(address as u32).to_le_bytes());
        }
        for (offset, value) in [
            (0x3fe6, 0x1234_u16),
            (0x6fde2, 2),
            (0x73fe2, 2),
            (0x6fdb2, 0xabcd),
            (0x6fdb4, 0xffff),
            (0x6fdb8, 0x9876),
            (0x6fdba, 0xffff),
        ] {
            image[offset..offset + 2].copy_from_slice(&value.to_le_bytes());
        }
        image
    }
}
