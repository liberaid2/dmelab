use std::collections::VecDeque;
use std::io::{self, Read, Write};
use std::time::Duration;

use dmelab_core::Transport;
use dmelab_ms43::{Error, Session};

/// Protocol fixture, without an OS serial device or simulated hardware timing.
struct ScriptedTransport {
    exchanges: VecDeque<(Vec<u8>, Vec<u8>)>,
    incoming: VecDeque<u8>,
    prepared: bool,
    timeout_configured: bool,
}

impl ScriptedTransport {
    fn new(exchanges: Vec<(Vec<u8>, Vec<u8>)>) -> Self {
        Self {
            exchanges: exchanges.into(),
            incoming: VecDeque::new(),
            prepared: false,
            timeout_configured: false,
        }
    }
}

impl Transport for ScriptedTransport {
    fn set_baud_rate(&mut self, _baud_rate: u32) -> io::Result<()> {
        panic!("default identification must not change baud rate");
    }

    fn set_timeout(&mut self, timeout: Duration) -> io::Result<()> {
        assert!(!timeout.is_zero());
        self.timeout_configured = true;
        Ok(())
    }

    fn clear_buffers(&mut self) -> io::Result<()> {
        assert!(self.timeout_configured);
        assert!(
            self.incoming.is_empty(),
            "clear must not discard an unread response"
        );
        self.prepared = true;
        Ok(())
    }
}

impl Write for ScriptedTransport {
    fn write(&mut self, buffer: &[u8]) -> io::Result<usize> {
        assert!(self.prepared);
        assert!(
            self.incoming.is_empty(),
            "previous response was not consumed"
        );
        let (expected, response) = self.exchanges.pop_front().expect("unexpected ECU command");
        assert_eq!(buffer, expected, "unexpected command or checksum");
        self.incoming.extend(buffer); // Adapter echo.
        self.incoming.extend(response);
        Ok(buffer.len())
    }

    fn flush(&mut self) -> io::Result<()> {
        Ok(())
    }
}

impl Read for ScriptedTransport {
    fn read(&mut self, buffer: &mut [u8]) -> io::Result<usize> {
        // Deliberately split echo, header, and payload across reads.
        let count = buffer.len().min(3).min(self.incoming.len());
        for byte in &mut buffer[..count] {
            *byte = self.incoming.pop_front().unwrap();
        }
        Ok(count)
    }
}

impl Drop for ScriptedTransport {
    fn drop(&mut self) {
        if !std::thread::panicking() {
            assert!(self.prepared);
            assert!(
                self.exchanges.is_empty(),
                "expected ECU command was not sent"
            );
            assert!(
                self.incoming.is_empty(),
                "ECU response was not fully consumed"
            );
        }
    }
}

fn response(data: &[u8]) -> Vec<u8> {
    let mut packet = vec![0x12, u8::try_from(data.len() + 4).unwrap(), 0xA0];
    packet.extend_from_slice(data);
    packet.push(packet.iter().fold(0, |sum, byte| sum ^ byte));
    packet
}

fn descriptor() -> Vec<u8> {
    let mut data = vec![0; 57];
    data[54..57].copy_from_slice(&[0x12, 0x34, 0x56]);
    response(&data)
}

#[test]
fn identification_uses_descriptor_pointer_and_exact_memory_requests() {
    let transport = ScriptedTransport::new(vec![
        (vec![0x12, 0x04, 0x0D, 0x1B], descriptor()),
        (
            vec![0x12, 0x09, 0x06, 0x00, 0x12, 0x34, 0x56, 0x08, 0x65],
            response(b"11143000"),
        ),
        (
            vec![0x12, 0x09, 0x06, 0x00, 0x0E, 0xFF, 0xCC, 0x08, 0x28],
            response(b"11143000"),
        ),
        (
            vec![0x12, 0x09, 0x06, 0x00, 0x0E, 0xFF, 0xBA, 0x06, 0x50],
            response(b"430069"),
        ),
    ]);
    let mut session = Session::new(transport, Duration::from_secs(1)).unwrap();
    let identification = session.identify().unwrap();
    assert_eq!(identification.hardware_id, "11143000");
    assert_eq!(identification.flash_identifier, "11143000");
    assert_eq!(identification.firmware_version, "430069");
}

#[test]
fn corrupt_descriptor_checksum_stops_before_any_memory_request() {
    let mut corrupt_descriptor = descriptor();
    *corrupt_descriptor.last_mut().unwrap() ^= 1;
    let transport =
        ScriptedTransport::new(vec![(vec![0x12, 0x04, 0x0D, 0x1B], corrupt_descriptor)]);
    let mut session = Session::new(transport, Duration::from_secs(1)).unwrap();
    assert!(matches!(session.identify(), Err(Error::InvalidChecksum)));
}
