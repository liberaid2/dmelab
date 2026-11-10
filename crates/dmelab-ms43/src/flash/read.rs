use super::commands::{NORMAL_TIMEOUT, ResponseCheck};
use super::image::{CALIBRATION_SIZE, FULL_SIZE};
use super::{FlashMode, FlashProgress};
use crate::{BaudRate, Error, Session};

impl Session {
    /// Read 512 KiB (including bootloader) or 64 KiB calibration, at a negotiated
    /// speed above 9600. Always attempt a negotiated return to 9600 afterwards.
    pub fn read_flash(
        &mut self,
        mode: FlashMode,
        baud_rate: BaudRate,
        progress: &mut dyn FnMut(FlashProgress),
    ) -> Result<Vec<u8>, Error> {
        self.identify_for_flash(baud_rate)?;
        self.change_baud_rate(baud_rate)?;
        let result = self.read_flash_at_speed(mode, progress);
        self.finish_flash(result)
    }

    fn read_flash_at_speed(
        &mut self,
        mode: FlashMode,
        progress: &mut dyn FnMut(FlashProgress),
    ) -> Result<Vec<u8>, Error> {
        let mut image = vec![
            0xff;
            if mode == FlashMode::Full {
                FULL_SIZE
            } else {
                CALIBRATION_SIZE
            }
        ];
        if mode == FlashMode::Full {
            self.read_flash_range(0, &mut image[..0xe000], "reading bootloader", progress)?;
            self.read_flash_range(
                0x90000,
                &mut image[0x10000..0x70000],
                "reading program",
                progress,
            )?;
            self.read_flash_range(
                0x70000,
                &mut image[0x70000..],
                "reading calibration",
                progress,
            )?;
        } else {
            self.read_flash_range(0x70000, &mut image, "reading calibration", progress)?;
        }
        Ok(image)
    }

    pub(super) fn read_flash_range(
        &mut self,
        mut address: u32,
        output: &mut [u8],
        stage: &'static str,
        progress: &mut dyn FnMut(FlashProgress),
    ) -> Result<(), Error> {
        let total = output.len();
        progress(FlashProgress {
            stage,
            completed: 0,
            total,
        });
        let mut completed = 0;
        for block in output.chunks_mut(123) {
            let bytes = address.to_be_bytes();
            let payload = [
                0x06,
                bytes[0],
                bytes[1],
                bytes[2],
                bytes[3],
                block.len() as u8,
            ];
            let response = self.flash_request(
                &payload,
                5,
                NORMAL_TIMEOUT,
                ResponseCheck::Data(block.len()),
            )?;
            block.copy_from_slice(&response[3..response.len() - 1]);
            address += block.len() as u32;
            completed += block.len();
            progress(FlashProgress {
                stage,
                completed,
                total,
            });
        }
        Ok(())
    }
}
