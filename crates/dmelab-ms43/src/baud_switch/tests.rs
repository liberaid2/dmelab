use std::collections::VecDeque;
use std::io::{self, Read, Write};
use std::time::{Duration, Instant};

use dmelab_core::Transport;

use super::*;

struct SpeedTransport {
    exchanges: VecDeque<(u32, Vec<u8>, Vec<u8>)>,
    incoming: VecDeque<u8>,
    speed_changes: VecDeque<u32>,
    current_speed: u32,
    acknowledged_at: Option<Instant>,
}

impl Read for SpeedTransport {
    fn read(&mut self, bytes: &mut [u8]) -> io::Result<usize> {
        let count = bytes.len().min(2).min(self.incoming.len());
        for byte in &mut bytes[..count] {
            *byte = self.incoming.pop_front().unwrap();
        }
        if self.incoming.is_empty() {
            self.acknowledged_at = Some(Instant::now());
        }
        Ok(count)
    }
}

impl Write for SpeedTransport {
    fn write(&mut self, bytes: &[u8]) -> io::Result<usize> {
        assert!(self.incoming.is_empty());
        let (speed, expected, response) = self.exchanges.pop_front().expect("unexpected command");
        assert_eq!(
            self.current_speed, speed,
            "request must use current UART speed"
        );
        assert_eq!(bytes, expected);
        self.incoming.extend(bytes);
        self.incoming.extend(response);
        Ok(bytes.len())
    }

    fn flush(&mut self) -> io::Result<()> {
        Ok(())
    }
}

impl Transport for SpeedTransport {
    fn set_timeout(&mut self, _timeout: Duration) -> io::Result<()> {
        Ok(())
    }

    fn clear_buffers(&mut self) -> io::Result<()> {
        assert!(self.incoming.is_empty());
        Ok(())
    }

    fn set_baud_rate(&mut self, baud_rate: u32) -> io::Result<()> {
        assert!(
            self.incoming.is_empty(),
            "must consume acknowledgement at old speed"
        );
        assert!(self.acknowledged_at.take().unwrap().elapsed() >= Duration::from_millis(200));
        assert_eq!(Some(baud_rate), self.speed_changes.pop_front());
        self.current_speed = baud_rate;
        Ok(())
    }
}

impl Drop for SpeedTransport {
    fn drop(&mut self) {
        if !std::thread::panicking() {
            assert!(self.exchanges.is_empty());
            assert!(self.incoming.is_empty());
            assert!(self.speed_changes.is_empty());
        }
    }
}

#[test]
fn speed_switch_waits_for_ack_then_changes_uart_and_restores_at_current_speed() {
    let transport = SpeedTransport {
        exchanges: VecDeque::from([
            (
                9600,
                vec![0x12, 8, 0x91, 1, 0xE8, 0x48, 0, 0x2A],
                vec![0x12, 4, 0xA0, 0xB6],
            ),
            (
                125000,
                vec![0x12, 8, 0x91, 0, 0x25, 0x80, 0, 0x2E],
                vec![0x12, 4, 0xA0, 0xB6],
            ),
        ]),
        incoming: VecDeque::new(),
        speed_changes: VecDeque::from([125000, 9600]),
        current_speed: 9600,
        acknowledged_at: None,
    };
    let mut session = Session::new(transport, Duration::from_secs(1)).unwrap();
    session.state = LinkState::Ready(BaudRate::B9600);
    session.change_baud_rate(BaudRate::B125000).unwrap();
    assert_eq!(session.baud_rate(), Some(BaudRate::B125000));
    session.restore_default_baud_rate().unwrap();
    assert_eq!(session.baud_rate(), Some(BaudRate::B9600));
}

#[test]
fn corrupt_speed_ack_does_not_change_uart_and_blocks_further_commands() {
    let transport = SpeedTransport {
        exchanges: VecDeque::from([(
            9600,
            vec![0x12, 8, 0x91, 1, 0xE8, 0x48, 0, 0x2A],
            vec![0x12, 4, 0xA0, 0xB7],
        )]),
        incoming: VecDeque::new(),
        speed_changes: VecDeque::new(),
        current_speed: 9600,
        acknowledged_at: None,
    };
    let mut session = Session::new(transport, Duration::from_secs(1)).unwrap();
    assert!(matches!(
        session.change_baud_rate(BaudRate::B125000),
        Err(Error::NotIdentified)
    ));
    session.state = LinkState::Ready(BaudRate::B9600);
    assert!(matches!(
        session.change_baud_rate(BaudRate::B125000),
        Err(Error::InvalidChecksum)
    ));
    assert_eq!(session.baud_rate(), None);
    assert!(matches!(session.identify(), Err(Error::Unsynchronized)));
    assert!(matches!(
        session.restore_default_baud_rate(),
        Err(Error::Unsynchronized)
    ));
}
