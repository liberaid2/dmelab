use std::time::Duration;

use crate::session::LinkState;
use crate::{Error, Session};

const PAGE_SIZE: usize = 10;
const RECORD_SIZE: usize = 10;
const PAGE_ATTEMPTS: usize = 4;
// MS4x Flasher UI field 0x04000148, forwarded to reader retry delay field C.
const RETRY_DELAY: Duration = Duration::from_millis(200);

#[cfg(test)]
mod tests;

/// MS43's native one-byte fault code, not an SAE Pxxxx code.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Dtc {
    pub code: u8,
    pub frequency: u8,
    /// Four uninterpreted data bytes; these are not decoded status flags.
    pub data: [u8; 4],
    /// Complete record, including fields whose meaning is not established.
    pub raw: [u8; RECORD_SIZE],
}

struct Page {
    total: u8,
    records: Vec<Dtc>,
}

impl Session {
    /// Read the complete fault memory, keeping the first record for each code.
    ///
    /// A fresh session is identified first. Uses the current negotiated speed;
    /// never changes speed, unlocks flash, or clears fault memory. Failed or
    /// inconsistent pages return an error, never a successful partial list.
    pub fn read_dtcs(&mut self) -> Result<Vec<Dtc>, Error> {
        self.identify_for_dtc()?;
        let mut expected_total = None;
        let mut records: Vec<Dtc> = Vec::new();
        // A wider cursor permits the final page at 251 without byte wraparound.
        for start in (1_u16..=255).step_by(PAGE_SIZE) {
            // The range guarantees that the wire index fits in one byte.
            let start_index = start as u8;
            let page = self
                .read_dtc_page(start_index, expected_total)
                .map_err(|source| Error::DtcRead {
                    start_index,
                    source: Box::new(source),
                })?;
            expected_total = Some(page.total);
            for record in page.records {
                if !records.iter().any(|previous| previous.code == record.code) {
                    records.push(record);
                }
            }
            if start + PAGE_SIZE as u16 > u16::from(page.total) {
                break;
            }
        }
        Ok(records)
    }

    /// Clear DTC memory once, requiring an A0 acknowledgement from the ECU.
    /// Identifies a fresh session first and preserves the current baud rate.
    /// No retries: a lost response does not prove the command was not executed.
    /// An acknowledgement does not guarantee that recurring faults stay absent.
    pub fn clear_dtcs(&mut self) -> Result<(), Error> {
        self.identify_for_dtc()?;
        self.protocol
            .prepare()
            .and_then(|()| self.protocol.request(&[0x05]).map(|_| ()))
            .map_err(|source| Error::DtcClear {
                source: Box::new(source),
            })
    }

    fn identify_for_dtc(&mut self) -> Result<(), Error> {
        match self.state {
            LinkState::Unidentified => {
                self.identify()?;
            }
            LinkState::Ready(_) => {}
            LinkState::Unsynchronized => return Err(Error::Unsynchronized),
        }
        Ok(())
    }

    fn read_dtc_page(&mut self, start: u8, total: Option<u8>) -> Result<Page, Error> {
        let mut attempt = 1;
        loop {
            // Discard remnants of an earlier timed-out/corrupt exchange before
            // retrying this same page. All requests are read-only.
            self.protocol.prepare()?;
            let result = self
                .protocol
                .request(&[0x04, start])
                .and_then(|response| parse_page(&response, start, total));
            match result {
                Ok(page) => return Ok(page),
                Err(error) => {
                    if attempt == PAGE_ATTEMPTS || !retryable(&error) {
                        return Err(error);
                    }
                    tracing::warn!(start_index = start, attempt, %error, "retrying DTC page");
                    std::thread::sleep(RETRY_DELAY);
                    attempt += 1;
                }
            }
        }
    }
}

fn retryable(error: &Error) -> bool {
    matches!(
        error,
        Error::Timeout { .. }
            | Error::EchoMismatch
            | Error::InvalidAddress(_)
            | Error::InvalidLength(_)
            | Error::InvalidChecksum
    )
}

/// Framing, echo, XOR and A0 status have already been checked by Protocol.
fn parse_page(response: &[u8], start: u8, expected_total: Option<u8>) -> Result<Page, Error> {
    if response.len() < 5 {
        return Err(Error::InvalidLength(response.len()));
    }
    let total = response[3];
    if let Some(expected) = expected_total
        && total != expected
    {
        return Err(Error::InvalidDtcPage(format!(
            "count changed from {expected} to {total}; read the complete list again"
        )));
    }
    let bytes = &response[4..response.len() - 1];
    let count = usize::from(total)
        .saturating_sub(usize::from(start) - 1)
        .min(PAGE_SIZE);
    // Strict completeness checks intentionally differ from Flasher's silent
    // truncation of short records and successful return of incomplete pages.
    if bytes.len() != count * RECORD_SIZE {
        return Err(Error::InvalidDtcPage(format!(
            "record {start}, total {total}: expected {} record bytes, received {}",
            count * RECORD_SIZE,
            bytes.len()
        )));
    }
    let records = bytes
        .chunks_exact(RECORD_SIZE)
        .map(|bytes| {
            let mut raw = [0; RECORD_SIZE];
            raw.copy_from_slice(bytes);
            Dtc {
                code: raw[0],
                frequency: raw[2],
                data: [raw[4], raw[5], raw[6], raw[7]],
                raw,
            }
        })
        .collect();
    Ok(Page { total, records })
}
