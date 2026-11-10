//! MS43 flash operations. Bootloader is readable but never erased or written.

mod checksums;
mod commands;
mod image;
mod read;
mod write;

pub use image::validate_image;

use crate::{BaudRate, Error, Identification, Session};

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum FlashMode {
    Full,
    Calibration,
}

#[derive(Debug, Clone, Copy)]
pub struct FlashProgress {
    pub stage: &'static str,
    pub completed: usize,
    pub total: usize,
}

impl Session {
    fn identify_for_flash(&mut self, baud_rate: BaudRate) -> Result<Identification, Error> {
        if baud_rate == BaudRate::B9600 {
            return Err(Error::Flash(
                "flash operations require a speed above 9600 baud".into(),
            ));
        }
        match self.baud_rate() {
            Some(BaudRate::B9600) => self.identify(),
            Some(_) => Err(Error::Flash("start flash operations at 9600 baud".into())),
            None => Err(Error::Unsynchronized),
        }
    }

    fn finish_flash<T>(&mut self, result: Result<T, Error>) -> Result<T, Error> {
        let restore = self.restore_default_baud_rate();
        match (result, restore) {
            (Ok(value), Ok(())) => Ok(value),
            (Err(error), Ok(())) | (Ok(_), Err(error)) => Err(error),
            (Err(operation), Err(restore)) => Err(Error::OperationAndRestore {
                operation: Box::new(operation),
                restore: Box::new(restore),
            }),
        }
    }
}
