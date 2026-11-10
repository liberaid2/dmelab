use std::collections::VecDeque;
use std::io::{self, Read, Write};
use std::sync::Arc;
use std::sync::atomic::{AtomicBool, Ordering};
use std::time::Instant;

use dmelab_core::Transport;

use super::*;

struct Exchange {
    payload: Vec<u8>,
    reply: Result<Vec<u8>, ErrorKind>,
}

struct Wire {
    exchanges: VecDeque<Exchange>,
    incoming: VecDeque<u8>,
    failure: Option<ErrorKind>,
    last_read: Option<Instant>,
    stop_after_measurements: Option<Arc<AtomicBool>>,
    stop_after_reply: bool,
    clear_failure: Option<Arc<AtomicBool>>,
}

impl Transport for Wire {
    fn set_timeout(&mut self, timeout: Duration) -> io::Result<()> {
        assert!(!timeout.is_zero());
        Ok(())
    }
    fn set_baud_rate(&mut self, _: u32) -> io::Result<()> {
        panic!("streaming must stay at 9600");
    }
    fn clear_buffers(&mut self) -> io::Result<()> {
        if self
            .clear_failure
            .as_ref()
            .is_some_and(|failed| failed.load(Ordering::Relaxed))
        {
            // serialport can lose unplug errno and expose only io::Other.
            return Err(io::Error::other("serial adapter unavailable"));
        }
        self.incoming.clear();
        self.failure = None;
        Ok(())
    }
}

impl Write for Wire {
    fn write(&mut self, bytes: &[u8]) -> io::Result<usize> {
        if let Some(last) = self.last_read {
            assert!(last.elapsed() >= Duration::from_millis(100));
        }
        let exchange = self.exchanges.pop_front().expect("unexpected request");
        let mut expected = vec![0x12, (exchange.payload.len() + 3) as u8];
        expected.extend(&exchange.payload);
        expected.push(expected.iter().fold(0, |sum, byte| sum ^ byte));
        assert_eq!(bytes, expected);
        assert!(self.incoming.is_empty(), "stale packet was not discarded");
        self.stop_after_reply = exchange.payload == [0x0b, 3];
        self.incoming.extend(bytes);
        match exchange.reply {
            Ok(reply) => self.incoming.extend(reply),
            Err(kind) => self.failure = Some(kind),
        }
        Ok(bytes.len())
    }
    fn flush(&mut self) -> io::Result<()> {
        Ok(())
    }
}

impl Read for Wire {
    fn read(&mut self, bytes: &mut [u8]) -> io::Result<usize> {
        if self.incoming.is_empty() {
            self.last_read = Some(Instant::now());
            return Err(self.failure.take().unwrap_or(ErrorKind::TimedOut).into());
        }
        let count = bytes.len().min(self.incoming.len()).min(3);
        for byte in &mut bytes[..count] {
            *byte = self.incoming.pop_front().unwrap();
        }
        if self.incoming.is_empty() {
            self.last_read = Some(Instant::now());
            if self.stop_after_reply
                && let Some(stop) = &self.stop_after_measurements
            {
                stop.store(true, Ordering::Relaxed);
            }
        }
        Ok(count)
    }
}

impl Drop for Wire {
    fn drop(&mut self) {
        if !std::thread::panicking() {
            assert!(self.exchanges.is_empty(), "expected requests not completed");
        }
    }
}

fn packet(data: &[u8]) -> Vec<u8> {
    let mut reply = vec![0x12, (data.len() + 4) as u8, 0xa0];
    reply.extend(data);
    reply.push(reply.iter().fold(0, |sum, byte| sum ^ byte));
    reply
}

fn exchange(payload: &[u8], data: &[u8]) -> Exchange {
    Exchange {
        payload: payload.to_vec(),
        reply: Ok(packet(data)),
    }
}

fn identification() -> Vec<Exchange> {
    let mut descriptor = vec![0; 57];
    descriptor[54..].copy_from_slice(&[0x12, 0x34, 0x56]);
    vec![
        exchange(&[0x0d], &descriptor),
        exchange(&[6, 0, 0x12, 0x34, 0x56, 8], b"11143000"),
        exchange(&[6, 0, 0x0e, 0xff, 0xcc, 8], b"11143000"),
        exchange(&[6, 0, 0x0e, 0xff, 0xba, 6], b"430069"),
    ]
}

fn session(exchanges: Vec<Exchange>, stop: Option<Arc<AtomicBool>>) -> Session {
    Session::new(wire(exchanges, stop), Duration::from_millis(100)).unwrap()
}

fn wire(exchanges: Vec<Exchange>, stop: Option<Arc<AtomicBool>>) -> Wire {
    Wire {
        exchanges: exchanges.into(),
        incoming: VecDeque::new(),
        failure: None,
        last_read: None,
        stop_after_measurements: stop,
        stop_after_reply: false,
        clear_failure: None,
    }
}

#[test]
fn stream_recovers_multiple_failed_samples_and_identifies_once() {
    let mut exchanges = vec![Exchange {
        payload: vec![0x0d],
        reply: Err(ErrorKind::TimedOut),
    }];
    exchanges.extend(identification());
    let mut bad = packet(&[0; 12]);
    *bad.last_mut().unwrap() ^= 1;
    bad.extend([0xfe; 8]); // unread garbage must be purged before the next request
    exchanges.extend([
        exchange(&[0x0b, 3], &[0; 41]),
        Exchange {
            payload: vec![0x0b, 0x90],
            reply: Ok(bad),
        },
        Exchange {
            payload: vec![0x0b, 3],
            reply: Err(ErrorKind::TimedOut),
        },
        exchange(&[0x0b, 3], &[0; 41]),
        exchange(&[0x0b, 0x90], &[0; 12]),
        exchange(&[0x0b, 4], &[0; 3]),
    ]);
    for _ in 0..2 {
        let mut measurements = [0; 41];
        measurements[..2].copy_from_slice(&1234_u16.to_be_bytes());
        exchanges.extend([
            exchange(&[0x0b, 3], &measurements),
            exchange(&[0x0b, 0x90], &[0; 12]),
            exchange(&[0x0b, 4], &[0, 0, 0, 4, 0, 0]),
        ]);
    }
    let mut session = session(exchanges, None);
    let mut failures = Vec::new();
    let mut samples = 0;
    session
        .stream_live_data(&|| false, &mut |event| {
            match event {
                LiveDataEvent::Recovering {
                    consecutive_failures,
                    ..
                } => failures.push(consecutive_failures),
                LiveDataEvent::Sample(data) => {
                    assert_eq!(data.engine_speed_rpm, 1234);
                    assert!(data.disa_command_active);
                    samples += 1;
                }
            }
            if samples == 2 {
                ControlFlow::Break(())
            } else {
                ControlFlow::Continue(())
            }
        })
        .unwrap();
    assert_eq!(failures, [1, 2, 3, 4]);
    assert_eq!(samples, 2);
    assert_eq!(session.baud_rate(), Some(BaudRate::B9600));
}

#[test]
fn stream_stops_between_blocks_and_reports_disconnection() {
    let stop = Arc::new(AtomicBool::new(false));
    let mut exchanges = identification();
    exchanges.push(exchange(&[0x0b, 3], &[0; 41]));
    let mut cancelled = session(exchanges, Some(Arc::clone(&stop)));
    cancelled
        .stream_live_data(&|| stop.load(Ordering::Relaxed), &mut |_| {
            panic!("cancelled/partial sample must not be published");
        })
        .unwrap();
    let mut disconnected = session(
        vec![Exchange {
            payload: vec![0x0d],
            reply: Err(ErrorKind::NotConnected),
        }],
        None,
    );
    assert!(matches!(disconnected.stream_live_data(&|| false, &mut |_| {
        panic!("disconnection must not be retried");
    }), Err(Error::Io { source, .. }) if source.kind() == ErrorKind::NotConnected));
}

// Essential regression: an unplugged handle must not be retried forever when
// the serial backend reports io::Other instead of a specific disconnect kind.
#[test]
fn stream_ends_when_buffer_cleanup_fails_after_a_successful_sample() {
    let unavailable = Arc::new(AtomicBool::new(false));
    let mut exchanges = identification();
    exchanges.extend([
        exchange(&[0x0b, 3], &[0; 41]),
        exchange(&[0x0b, 0x90], &[0; 12]),
        exchange(&[0x0b, 4], &[0; 6]),
    ]);
    let mut transport = wire(exchanges, None);
    transport.clear_failure = Some(Arc::clone(&unavailable));
    let mut session = Session::new(transport, Duration::from_millis(100)).unwrap();
    let mut samples = 0;
    let mut retried = false;
    let result = session.stream_live_data(&|| false, &mut |event| match event {
        LiveDataEvent::Sample(_) => {
            samples += 1;
            unavailable.store(true, Ordering::Relaxed);
            ControlFlow::Continue(())
        }
        LiveDataEvent::Recovering { .. } => {
            retried = true;
            ControlFlow::Break(())
        }
    });
    assert!(matches!(result,
        Err(Error::LiveDataRead { source, .. })
        if matches!(source.as_ref(), Error::TransportSetup { source, .. } if source.kind() == ErrorKind::Other)
    ));
    assert_eq!(samples, 1);
    assert!(!retried);
}

#[test]
fn selected_stream_recovers_and_keeps_request_gaps_between_single_block_samples() {
    let mut exchanges = identification();
    exchanges.push(Exchange {
        payload: vec![0x0b, 3],
        reply: Err(ErrorKind::TimedOut),
    });
    for rpm in [1234_u16, 2345] {
        let mut measurements = [0; 41];
        measurements[..2].copy_from_slice(&rpm.to_be_bytes());
        exchanges.push(exchange(&[0x0b, 3], &measurements));
    }
    let mut session = session(exchanges, None);
    let selection = LiveDataSelection {
        measurements: true,
        vanos: false,
        digital: false,
        pedal: false,
        rough_running: false,
    };
    let mut samples = Vec::new();
    let mut failures = 0;
    session
        .stream_live_data_selected(selection, &|| false, &mut |event| {
            match event {
                SelectedLiveDataEvent::Sample(data) => {
                    assert!(data.vanos.is_none());
                    assert!(data.digital.is_none());
                    samples.push(data.measurements.unwrap().engine_speed_rpm);
                }
                SelectedLiveDataEvent::Recovering {
                    consecutive_failures,
                    error,
                } => {
                    assert!(matches!(error, Error::LiveDataRead { block: 3, .. }));
                    assert_eq!(consecutive_failures, 1);
                    failures += 1;
                }
            }
            if samples.len() == 2 {
                ControlFlow::Break(())
            } else {
                ControlFlow::Continue(())
            }
        })
        .unwrap();
    assert_eq!(samples, [1234, 2345]);
    assert_eq!(failures, 1);
}
