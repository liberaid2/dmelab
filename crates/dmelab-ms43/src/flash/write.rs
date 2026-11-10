use super::checksums::correct_checksums;
use super::commands::{NORMAL_TIMEOUT, ResponseCheck};
use super::image::{CALIBRATION_SIZE, image_firmware_version, validate_image};
use super::{FlashMode, FlashProgress};
use crate::{BaudRate, Error, Session};

#[derive(Debug, Clone, Copy)]
struct WriteSection {
    address: u32,
    offset: usize,
    length: usize,
    stage: &'static str,
}

fn write_sections(mode: FlashMode, image_length: usize) -> Vec<WriteSection> {
    let mut sections = Vec::new();
    if mode == FlashMode::Full {
        sections.push(WriteSection {
            address: 0x90000,
            offset: 0x10000,
            length: 0x5fff0,
            stage: "writing program",
        });
    }
    sections.push(WriteSection {
        address: 0x70000,
        offset: if image_length == CALIBRATION_SIZE {
            0
        } else {
            0x70000
        },
        length: 0xffef,
        stage: "writing calibration",
    });
    sections
}

impl Session {
    /// Correct checksums and write program/calibration or calibration only.
    /// Never erases or writes bootloader. The input image is not modified.
    /// Requires firmware compatibility for calibration and >=11.5 V from ECU.
    pub fn write_flash(
        &mut self,
        mode: FlashMode,
        image: &[u8],
        baud_rate: BaudRate,
        progress: &mut dyn FnMut(FlashProgress),
    ) -> Result<(), Error> {
        validate_image(image, mode)?;
        let identification = self.identify_for_flash(baud_rate)?;
        if mode == FlashMode::Calibration {
            let firmware = image_firmware_version(image)?;
            if firmware != identification.firmware_version {
                return Err(Error::FirmwareMismatch {
                    image: firmware,
                    ecu: identification.firmware_version,
                });
            }
        }
        self.check_flash_voltage()?;
        let context = if image.len() == CALIBRATION_SIZE {
            Some(self.read_memory(0x0efdb8, 40)?)
        } else {
            None
        };
        let mut corrected = image.to_vec();
        correct_checksums(&mut corrected, context.as_deref())?;
        self.change_baud_rate(baud_rate)?;
        let result = self.write_flash_at_speed(mode, &corrected, progress);
        self.finish_flash(result)
    }

    fn check_flash_voltage(&mut self) -> Result<(), Error> {
        // communication 0xDC delegates to 0xBE: request 0B03, expected frame
        // >=45 bytes, voltage byte25 multiplied by decimal 0.10196078 V.
        self.protocol.prepare()?;
        let response = self.protocol.request(&[0x0b, 0x03])?;
        if response.len() < 45 {
            return Err(Error::InvalidLength(response.len()));
        }
        let voltage = u64::from(response[25]) * 10_196_078;
        let millivolts = (voltage / 100_000) as u32;
        tracing::info!(millivolts, "ECU supply voltage");
        if voltage < 1_150_000_000 {
            return Err(Error::LowVoltage { millivolts });
        }
        Ok(())
    }

    fn write_flash_at_speed(
        &mut self,
        mode: FlashMode,
        image: &[u8],
        progress: &mut dyn FnMut(FlashProgress),
    ) -> Result<(), Error> {
        progress(FlashProgress {
            stage: "unlocking ECU",
            completed: 0,
            total: 1,
        });
        self.unlock_flash()?;
        let sections = write_sections(mode, image.len());
        self.finalize_flash_section(sections[0].address, false)?;
        for section in sections {
            let erase_stage = if section.address == 0x90000 {
                "erasing program"
            } else {
                "erasing calibration"
            };
            progress(FlashProgress {
                stage: erase_stage,
                completed: 0,
                total: 1,
            });
            self.erase_flash_section(section.address)?;
            self.write_flash_section(image, section, progress)?;
            progress(FlashProgress {
                stage: "finalizing flash",
                completed: 0,
                total: 1,
            });
            self.finalize_flash_section(section.address, section.address == 0x70000)?;
        }
        progress(FlashProgress {
            stage: "flash completed",
            completed: 1,
            total: 1,
        });
        Ok(())
    }

    fn write_flash_section(
        &mut self,
        image: &[u8],
        section: WriteSection,
        progress: &mut dyn FnMut(FlashProgress),
    ) -> Result<(), Error> {
        let mut completed = 0;
        progress(FlashProgress {
            stage: section.stage,
            completed,
            total: section.length,
        });
        while completed < section.length {
            // Original checks an FF pair even for the last odd logical byte.
            while completed < section.length
                && image[section.offset + completed..section.offset + completed + 2] == [0xff, 0xff]
            {
                completed += 2;
            }
            if completed >= section.length {
                break;
            }
            let mut length = 118.min(section.length - completed);
            let offset = section.offset + completed;
            while length > 2 && image[offset + length - 2..offset + length] == [0xff, 0xff] {
                length -= 2;
            }
            let address = (section.address + completed as u32).to_be_bytes();
            let mut payload = vec![0x07, 0x02, address[1], address[2], address[3], length as u8];
            payload.extend_from_slice(&image[offset..offset + length]);
            self.flash_request(&payload, 10, NORMAL_TIMEOUT, ResponseCheck::Verified)?;
            completed += length;
            progress(FlashProgress {
                stage: section.stage,
                completed,
                total: section.length,
            });
        }
        progress(FlashProgress {
            stage: section.stage,
            completed: section.length,
            total: section.length,
        });
        Ok(())
    }
}
