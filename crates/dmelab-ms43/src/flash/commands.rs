//! Transaction checks and programming commands from communication 0x16–0x1e.

use std::time::{Duration, Instant};

use crate::{Error, Session};

pub(super) const RETRY_DELAY: Duration = Duration::from_millis(200);
pub(super) const NORMAL_TIMEOUT: Duration = Duration::from_secs(1);
const ERASE_TIMEOUT: Duration = Duration::from_secs(60);

#[derive(Clone, Copy)]
pub(super) enum ResponseCheck {
    Accepted,
    Data(usize),
    Seed,
    Verified,
}

impl Session {
    pub(super) fn flash_request(
        &mut self,
        payload: &[u8],
        attempts: usize,
        timeout: Duration,
        check: ResponseCheck,
    ) -> Result<Vec<u8>, Error> {
        for attempt in 1..=attempts {
            let result = self.protocol.prepare().and_then(|()| {
                let response = self.protocol.request_raw_with_timeout(payload, timeout)?;
                check_response(&response, check)?;
                Ok(response)
            });
            match result {
                Ok(response) => return Ok(response),
                Err(error) if attempt == attempts => return Err(error),
                Err(error) => {
                    tracing::warn!(attempt, %error, "retry MS43 flash request");
                    std::thread::sleep(RETRY_DELAY);
                }
            }
        }
        Err(Error::Flash("flash request has no attempts".into()))
    }

    pub(super) fn unlock_flash(&mut self) -> Result<(), Error> {
        // Original chooses r in 1..24. A fixed member of that range implements
        // the same challenge calculation without adding an RNG dependency.
        let r = 1usize;
        let seed = self.flash_request(
            &[0x90, 0x42, 0x4d, 0x57, r as u8],
            10,
            NORMAL_TIMEOUT,
            ResponseCheck::Seed,
        )?;
        if seed.len() == 5 {
            return Ok(());
        }
        let mut payload = [0x90, 0, 0, 0, 0];
        for i in 0..4 {
            payload[i + 1] = seed[(r + i) % seed.len()]
                .wrapping_add(seed[18 + i])
                .wrapping_add(seed[41 + i]);
        }
        self.flash_request(&payload, 10, NORMAL_TIMEOUT, ResponseCheck::Accepted)?;
        Ok(())
    }

    pub(super) fn erase_flash_section(&mut self, address: u32) -> Result<(), Error> {
        require_writable_section(address)?;
        self.flash_request(
            &section_command(0x06, address),
            10,
            ERASE_TIMEOUT,
            ResponseCheck::Verified,
        )?;
        Ok(())
    }

    pub(super) fn finalize_flash_section(
        &mut self,
        address: u32,
        verify: bool,
    ) -> Result<(), Error> {
        require_writable_section(address)?;
        let payload = section_command(0x0f, address);
        // Original A1 polling is unbounded. Bound a single preparation/finalize
        // stage to the original long timeout, independently of retry count.
        let started = Instant::now();
        let mut errors = 0;
        loop {
            let remaining = ERASE_TIMEOUT
                .checked_sub(started.elapsed())
                .filter(|duration| !duration.is_zero())
                .ok_or(Error::Timeout {
                    stage: "finalize flash",
                })?;
            self.protocol.prepare()?;
            let result = self
                .protocol
                .request_raw_with_timeout(&payload, NORMAL_TIMEOUT.min(remaining));
            let result = match result {
                Ok(response) if response[2] == 0xa1 => {
                    std::thread::sleep(RETRY_DELAY.min(remaining));
                    continue;
                }
                Ok(response) => check_response(
                    &response,
                    if verify {
                        ResponseCheck::Verified
                    } else {
                        ResponseCheck::Accepted
                    },
                ),
                Err(error) => Err(error),
            };
            match result {
                Ok(()) => return Ok(()),
                Err(error) => {
                    errors += 1;
                    if errors == 10 {
                        return Err(error);
                    }
                    std::thread::sleep(RETRY_DELAY.min(remaining));
                }
            }
        }
    }
}

fn require_writable_section(address: u32) -> Result<(), Error> {
    if matches!(address, 0x90000 | 0x70000) {
        Ok(())
    } else {
        Err(Error::Flash(format!(
            "section 0x{address:06X} is not writable; bootloader programming is forbidden"
        )))
    }
}

fn check_response(response: &[u8], check: ResponseCheck) -> Result<(), Error> {
    if response[2] != 0xa0 {
        return Err(Error::Rejected(response[2]));
    }
    match check {
        ResponseCheck::Data(length) if response.len() != length + 4 => {
            Err(Error::InvalidLength(response.len()))
        }
        ResponseCheck::Seed if response.len() != 5 && response.len() != 46 => {
            Err(Error::InvalidLength(response.len()))
        }
        ResponseCheck::Verified => {
            // Index 8 must be payload, not the packet checksum.
            if response.len() < 10 {
                return Err(Error::InvalidLength(response.len()));
            }
            let status = response[8];
            if status == 1 {
                return Ok(());
            }
            let description = match status {
                2 => "transfer verification failed; retry with a lower baud rate",
                9 => "hardware reference error",
                10 => "program reference error",
                11 => "hardware/program references do not match",
                12 => "program section incomplete",
                13 => "calibration reference error",
                14 => "program/calibration references do not match",
                15 => "calibration section incomplete",
                _ => "unexpected verification response",
            };
            Err(Error::Flash(format!("{description} (0x{status:02X})")))
        }
        _ => Ok(()),
    }
}

pub(super) fn section_command(command: u8, address: u32) -> [u8; 6] {
    let address = address.to_be_bytes();
    [0x07, command, address[1], address[2], address[3], 0]
}
