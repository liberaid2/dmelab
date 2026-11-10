use std::collections::VecDeque;
use std::io::{self, Read, Write};
use std::time::Duration;

use dmelab_core::Transport;

use super::*;
use crate::BaudRate;
use crate::protocol::Protocol;

struct ScriptedTransport {
    exchanges: VecDeque<(Vec<u8>, Option<Vec<u8>>)>,
    incoming: VecDeque<u8>,
}

impl Transport for ScriptedTransport {
    fn set_timeout(&mut self, timeout: Duration) -> io::Result<()> {
        assert!(!timeout.is_zero());
        Ok(())
    }

    fn set_baud_rate(&mut self, _: u32) -> io::Result<()> {
        panic!("DTC operations must not change speed");
    }

    fn clear_buffers(&mut self) -> io::Result<()> {
        self.incoming.clear();
        Ok(())
    }
}

impl Write for ScriptedTransport {
    fn write(&mut self, bytes: &[u8]) -> io::Result<usize> {
        let (expected, reply) = self.exchanges.pop_front().expect("unexpected request");
        assert_eq!(bytes, expected);
        self.incoming.extend(bytes);
        if let Some(reply) = reply {
            self.incoming.extend(reply);
        }
        Ok(bytes.len())
    }

    fn flush(&mut self) -> io::Result<()> {
        Ok(())
    }
}

impl Read for ScriptedTransport {
    fn read(&mut self, bytes: &mut [u8]) -> io::Result<usize> {
        if self.incoming.is_empty() {
            return Err(io::ErrorKind::TimedOut.into());
        }
        let count = bytes.len().min(3).min(self.incoming.len());
        for byte in &mut bytes[..count] {
            *byte = self.incoming.pop_front().unwrap();
        }
        Ok(count)
    }
}

impl Drop for ScriptedTransport {
    fn drop(&mut self) {
        if !std::thread::panicking() {
            assert!(self.exchanges.is_empty(), "expected request was not sent");
            assert!(self.incoming.is_empty(), "response was not consumed");
        }
    }
}

fn frame(total: u8, records: &[u8]) -> Vec<u8> {
    let mut bytes = vec![0x12, u8::try_from(records.len() + 5).unwrap(), 0xA0, total];
    bytes.extend(records);
    bytes.push(bytes.iter().fold(0, |sum, byte| sum ^ byte));
    bytes
}

fn session(exchanges: Vec<(u8, Option<Vec<u8>>)>) -> Session {
    packet_session(
        exchanges
            .into_iter()
            .map(|(start, reply)| (vec![0x12, 5, 4, start, 0x12 ^ 5 ^ 4 ^ start], reply))
            .collect(),
    )
}

fn packet_session(exchanges: Vec<(Vec<u8>, Option<Vec<u8>>)>) -> Session {
    let transport = ScriptedTransport {
        exchanges: exchanges.into(),
        incoming: VecDeque::new(),
    };
    // Skip existing identification coverage to isolate the DTC protocol contract.
    Session {
        protocol: Protocol::new(transport, Duration::from_secs(1)).unwrap(),
        state: LinkState::Ready(BaudRate::B9600),
    }
}

#[test]
fn clear_requires_acknowledgement_and_never_retries_a_lost_or_negative_response() {
    let request = vec![0x12, 4, 5, 0x13];
    let mut confirmed = packet_session(vec![(request.clone(), Some(vec![0x12, 4, 0xA0, 0xB6]))]);
    confirmed.clear_dtcs().unwrap();

    let mut rejected = packet_session(vec![(request.clone(), Some(vec![0x12, 4, 0xB0, 0xA6]))]);
    assert!(
        matches!(rejected.clear_dtcs(), Err(Error::DtcClear { source })
        if matches!(*source, Error::Rejected(0xB0)))
    );

    let mut lost_ack = packet_session(vec![(request, None)]);
    assert!(
        matches!(lost_ack.clear_dtcs(), Err(Error::DtcClear { source })
        if matches!(*source, Error::Timeout { .. }))
    );
}

#[test]
fn retries_the_failed_page_and_finishes_at_255_without_cursor_wrap() {
    let mut exchanges = Vec::new();
    for start in (1_u16..=255).step_by(10) {
        if start == 11 {
            exchanges.push((11, None));
        }
        let mut records = Vec::new();
        for index in start..=(start + 9).min(255) {
            // Last record duplicates the first code, with different frequency.
            let code = if index == 255 { 1 } else { index as u8 };
            records.extend([code, 0x80, index as u8, 0x40, 0xA, 0xB, 0, 0xFF, 0x55, 0xAA]);
        }
        exchanges.push((start as u8, Some(frame(255, &records))));
    }
    let records = session(exchanges).read_dtcs().unwrap();
    assert_eq!(records.len(), 254);
    assert_eq!(records[0].code, 1);
    assert_eq!(records[0].frequency, 1);
    assert_eq!(records[0].data, [0xA, 0xB, 0, 0xFF]);
    assert_eq!(
        records[0].raw,
        [1, 0x80, 1, 0x40, 0xA, 0xB, 0, 0xFF, 0x55, 0xAA]
    );
    assert_eq!(records.last().unwrap().code, 254);
}

#[test]
fn failed_or_incomplete_pages_never_return_successful_partial_results() {
    let mut exchanges = vec![(1, Some(frame(11, &[0; 100])))];
    exchanges.extend((0..PAGE_ATTEMPTS).map(|_| (11, None)));
    let error = session(exchanges).read_dtcs().unwrap_err();
    assert!(matches!(error, Error::DtcRead { start_index: 11, source }
        if matches!(*source, Error::Timeout { .. })));

    let error = session(vec![(1, Some(frame(1, &[0; 9])))])
        .read_dtcs()
        .unwrap_err();
    assert!(matches!(error, Error::DtcRead { start_index: 1, source }
        if matches!(*source, Error::InvalidDtcPage(_))));

    // A structurally valid zero-count page is the only successful empty result.
    assert!(
        session(vec![(1, Some(frame(0, &[])))])
            .read_dtcs()
            .unwrap()
            .is_empty()
    );
}
