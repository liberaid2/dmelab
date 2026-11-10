use std::collections::VecDeque;
use std::io::{self, Read, Write};
use std::time::Duration;

use dmelab_core::Transport;
use dmelab_ms43::{BaudRate, FlashMode, Session};

/// Essential destructive-operation check: exact public-API wire exchanges,
/// including checksums, section boundaries and negotiated speed restoration.
/// This emulates packets only; it does not establish hardware compatibility.
#[test]
fn full_and_partial_write_never_erase_or_write_bootloader() {
    let original = image_fixture();
    let mut corrected = original.clone();
    // Independently verified against the Python port and original CIL
    // interpreter; same checksum vector as the essential checksum unit test.
    for (offset, value) in [
        (0x3c24, 0x15e2_u16),
        (0x6fdae, 0x8a4d),
        (0x6fdb0, 0x001e),
        (0x6fde0, 0x07af),
        (0x72ffc, 0x17cb),
        (0x72ffe, 0x001d),
        (0x73fe0, 0x94ae),
    ] {
        corrected[offset..offset + 2].copy_from_slice(&value.to_le_bytes());
    }

    for mode in [FlashMode::Full, FlashMode::Calibration] {
        let mut exchanges = identification();
        let mut voltage = vec![0; 41];
        voltage[22] = 130;
        exchanges.push((vec![0x0b, 0x03], voltage));
        if mode == FlashMode::Calibration {
            exchanges.push((
                vec![0x06, 0x00, 0x0e, 0xfd, 0xb8, 40],
                original[0x6fdb8..0x6fde0].to_vec(),
            ));
        }
        exchanges.push((vec![0x91, 0x01, 0xe8, 0x48, 0], vec![]));
        exchanges.push((vec![0x90, 0x42, 0x4d, 0x57, 1], vec![0]));
        let first = if mode == FlashMode::Full {
            0x90000
        } else {
            0x70000
        };
        exchanges.push((section(0x0f, first), vec![]));
        if mode == FlashMode::Full {
            append_section(&mut exchanges, 0x90000, &corrected[0x10000..0x6fff0], false);
        }
        append_section(&mut exchanges, 0x70000, &corrected[0x70000..0x7ffef], true);
        exchanges.push((vec![0x91, 0x00, 0x25, 0x80, 0], vec![]));

        let transport = FlashTransport {
            exchanges: exchanges.into(),
            incoming: VecDeque::new(),
            baud_changes: VecDeque::from([125000, 9600]),
            baud_rate: 9600,
        };
        let mut session = Session::new(transport, Duration::from_secs(1)).unwrap();
        let input = if mode == FlashMode::Full {
            &original[..]
        } else {
            &original[0x70000..]
        };
        let unchanged = input.to_vec();
        session
            .write_flash(mode, input, BaudRate::B125000, &mut |_| {})
            .unwrap();
        assert_eq!(session.baud_rate(), Some(BaudRate::B9600));
        assert_eq!(
            input, unchanged,
            "writing must not mutate the caller's image"
        );
    }
}

#[test]
fn full_and_partial_read_map_ecu_sections_and_preserve_full_image_hole() {
    let source = image_fixture();
    for mode in [FlashMode::Full, FlashMode::Calibration] {
        let mut exchanges = identification();
        exchanges.push((vec![0x91, 0x01, 0xe8, 0x48, 0], vec![]));
        if mode == FlashMode::Full {
            append_read(&mut exchanges, 0, &source[..0xe000]);
            append_read(&mut exchanges, 0x90000, &source[0x10000..0x70000]);
        }
        append_read(&mut exchanges, 0x70000, &source[0x70000..]);
        exchanges.push((vec![0x91, 0, 0x25, 0x80, 0], vec![]));
        let transport = FlashTransport {
            exchanges: exchanges.into(),
            incoming: VecDeque::new(),
            baud_changes: VecDeque::from([125000, 9600]),
            baud_rate: 9600,
        };
        let mut session = Session::new(transport, Duration::from_secs(1)).unwrap();
        let actual = session
            .read_flash(mode, BaudRate::B125000, &mut |_| {})
            .unwrap();
        let expected = if mode == FlashMode::Full {
            let mut expected = source.clone();
            expected[0xe000..0x10000].fill(0xff);
            expected
        } else {
            source[0x70000..].to_vec()
        };
        assert_eq!(actual, expected);
        assert_eq!(session.baud_rate(), Some(BaudRate::B9600));
    }
}

fn append_read(exchanges: &mut Vec<(Vec<u8>, Vec<u8>)>, address: u32, bytes: &[u8]) {
    for (index, block) in bytes.chunks(123).enumerate() {
        let mut payload = vec![6];
        payload.extend_from_slice(&(address + (index * 123) as u32).to_be_bytes());
        payload.push(block.len() as u8);
        exchanges.push((payload, block.to_vec()));
    }
}

fn identification() -> Vec<(Vec<u8>, Vec<u8>)> {
    let mut descriptor = vec![0; 57];
    descriptor[54..57].copy_from_slice(&[0x12, 0x34, 0x56]);
    vec![
        (vec![0x0d], descriptor),
        (vec![6, 0, 0x12, 0x34, 0x56, 8], b"11143000".to_vec()),
        (vec![6, 0, 0x0e, 0xff, 0xcc, 8], b"11143000".to_vec()),
        (vec![6, 0, 0x0e, 0xff, 0xba, 6], b"430069".to_vec()),
    ]
}

fn section(command: u8, address: u32) -> Vec<u8> {
    let address = address.to_be_bytes();
    vec![7, command, address[1], address[2], address[3], 0]
}

fn verified() -> Vec<u8> {
    vec![0, 0, 0, 0, 0, 1]
}

fn append_section(
    exchanges: &mut Vec<(Vec<u8>, Vec<u8>)>,
    address: u32,
    bytes: &[u8],
    final_verification: bool,
) {
    assert!(matches!(
        (address, bytes.len()),
        (0x90000, 0x5fff0) | (0x70000, 0xffef)
    ));
    exchanges.push((section(6, address), verified()));
    // This dense fixture has no FF pairs at block boundaries: every writable
    // byte must be transferred, including the final odd calibration byte.
    for (index, block) in bytes.chunks(118).enumerate() {
        let mut payload = section(2, address + (index * 118) as u32);
        payload[5] = block.len() as u8;
        payload.extend_from_slice(block);
        exchanges.push((payload, verified()));
    }
    exchanges.push((
        section(0x0f, address),
        if final_verification {
            verified()
        } else {
            vec![]
        },
    ));
}

struct FlashTransport {
    exchanges: VecDeque<(Vec<u8>, Vec<u8>)>,
    incoming: VecDeque<u8>,
    baud_changes: VecDeque<u32>,
    baud_rate: u32,
}

impl Transport for FlashTransport {
    fn set_baud_rate(&mut self, baud_rate: u32) -> io::Result<()> {
        assert!(
            self.incoming.is_empty(),
            "consume ECU acknowledgement before switching UART"
        );
        assert_eq!(self.baud_changes.pop_front(), Some(baud_rate));
        self.baud_rate = baud_rate;
        Ok(())
    }

    fn set_timeout(&mut self, timeout: Duration) -> io::Result<()> {
        assert!(!timeout.is_zero());
        Ok(())
    }

    fn clear_buffers(&mut self) -> io::Result<()> {
        assert!(self.incoming.is_empty(), "must not discard unread ECU data");
        Ok(())
    }
}

impl Write for FlashTransport {
    fn write(&mut self, bytes: &[u8]) -> io::Result<usize> {
        assert!(self.incoming.is_empty());
        let (expected, data) = self.exchanges.pop_front().expect("unexpected ECU command");
        assert_eq!(
            bytes,
            frame(&expected),
            "unexpected command, address, length or data"
        );
        if matches!(expected[0], 7 | 0x90) {
            assert_eq!(
                self.baud_rate, 125000,
                "programming requires negotiated acceleration"
            );
        }
        self.incoming.extend(bytes);
        let mut response = vec![0xa0];
        response.extend(data);
        self.incoming.extend(frame(&response));
        Ok(bytes.len())
    }

    fn flush(&mut self) -> io::Result<()> {
        Ok(())
    }
}

impl Read for FlashTransport {
    fn read(&mut self, buffer: &mut [u8]) -> io::Result<usize> {
        let count = buffer.len().min(3).min(self.incoming.len());
        for byte in &mut buffer[..count] {
            *byte = self.incoming.pop_front().unwrap();
        }
        Ok(count)
    }
}

impl Drop for FlashTransport {
    fn drop(&mut self) {
        if !std::thread::panicking() {
            assert!(
                self.exchanges.is_empty(),
                "expected programming exchanges did not complete"
            );
            assert!(self.incoming.is_empty());
            assert!(self.baud_changes.is_empty(), "UART must return to 9600");
        }
    }
}

fn frame(payload: &[u8]) -> Vec<u8> {
    let mut bytes = vec![0x12, (payload.len() + 3) as u8];
    bytes.extend_from_slice(payload);
    bytes.push(bytes.iter().fold(0, |xor, byte| xor ^ byte));
    bytes
}

fn image_fixture() -> Vec<u8> {
    let mut image: Vec<u8> = (0..0x80000).map(|i| (i * 13 + 7) as u8).collect();
    image[0x6ffcc..0x6ffd4].copy_from_slice(b"111430xx");
    image[0x70020..0x70028].copy_from_slice(b"111430xx");
    image[0x6ffba..0x6ffc0].copy_from_slice(b"430069");
    image[0x70008..0x7000e].copy_from_slice(b"430069");
    for (offset, address) in [
        (0x3c28, 0x200_u32),
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
        image[offset..offset + 4].copy_from_slice(&address.to_le_bytes());
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
