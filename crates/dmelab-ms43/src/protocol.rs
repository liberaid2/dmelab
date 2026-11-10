use std::io;
use std::time::{Duration, Instant};

use dmelab_core::Transport;

use crate::Error;

const ADDRESS: u8 = 0x12;

pub(crate) struct Protocol {
    transport: Box<dyn Transport>,
    timeout: Duration,
    request_gap: Duration,
    last_exchange: Option<Instant>,
}

impl Protocol {
    pub(crate) fn set_baud_rate(&mut self, baud_rate: u32) -> io::Result<()> {
        self.transport.set_baud_rate(baud_rate)
    }

    pub(crate) fn new(
        transport: impl Transport + 'static,
        timeout: Duration,
    ) -> Result<Self, Error> {
        if !(Duration::from_millis(1)..=Duration::from_secs(60)).contains(&timeout) {
            return Err(Error::InvalidTimeout);
        }
        Ok(Self {
            transport: Box::new(transport),
            timeout,
            request_gap: Duration::ZERO,
            last_exchange: None,
        })
    }

    pub(crate) fn set_request_gap(&mut self, gap: Duration) -> Duration {
        std::mem::replace(&mut self.request_gap, gap)
    }

    pub(crate) fn wait_after_exchange(&self, gap: Duration) {
        if let Some(last) = self.last_exchange {
            let remaining = gap.saturating_sub(last.elapsed());
            if !remaining.is_zero() {
                std::thread::sleep(remaining);
            }
        }
    }

    pub(crate) fn prepare(&mut self) -> Result<(), Error> {
        // Purge after the recovery gap, so late bytes are discarded before TX.
        self.wait_after_exchange(self.request_gap);
        self.configure_timeout(self.timeout)?;
        self.transport
            .clear_buffers()
            .map_err(|source| Error::TransportSetup {
                stage: "clear buffers",
                source,
            })
    }

    pub(crate) fn request(&mut self, payload: &[u8]) -> Result<Vec<u8>, Error> {
        let response = self.request_raw(payload)?;
        if response[2] != 0xA0 {
            return Err(Error::Rejected(response[2]));
        }
        Ok(response)
    }

    /// A framed exchange with status interpreted by the caller (e.g. flash A1).
    pub(crate) fn request_raw_with_timeout(
        &mut self,
        payload: &[u8],
        timeout: Duration,
    ) -> Result<Vec<u8>, Error> {
        if timeout.is_zero() || timeout > Duration::from_secs(60) {
            return Err(Error::InvalidTimeout);
        }
        let previous = self.timeout;
        self.timeout = timeout;
        let result = self.request_raw(payload);
        self.timeout = previous;
        result
    }

    pub(crate) fn request_raw(&mut self, payload: &[u8]) -> Result<Vec<u8>, Error> {
        let packet = encode(payload)?;
        self.wait_after_exchange(self.request_gap);
        let result = self.exchange(&packet);
        // Failed/partial packets also require a recovery gap before the next TX.
        self.last_exchange = Some(Instant::now());
        result
    }

    fn exchange(&mut self, packet: &[u8]) -> Result<Vec<u8>, Error> {
        let started = Instant::now();
        self.configure_timeout(self.timeout)?;
        tracing::debug!(tx = ?packet, "K-line request");
        self.write_request(packet, started)?;

        let mut echo = vec![0; packet.len()];
        self.read_exact(&mut echo, started, "read echo")?;
        if echo != packet {
            return Err(Error::EchoMismatch);
        }
        let response = self.response(started)?;
        tracing::debug!(rx = ?response, "K-line response");
        Ok(response)
    }

    fn write_request(&mut self, mut bytes: &[u8], started: Instant) -> Result<(), Error> {
        while !bytes.is_empty() {
            self.remaining_timeout(started, "write request")?;
            match self.transport.write(bytes) {
                Ok(0) => {
                    return Err(Error::io(
                        "write request",
                        io::Error::from(io::ErrorKind::WriteZero),
                    ));
                }
                Ok(count) => bytes = &bytes[count..],
                Err(error) if error.kind() == io::ErrorKind::Interrupted => continue,
                Err(error) => return Err(Error::io("write request", error)),
            }
        }
        Ok(())
    }

    fn response(&mut self, started: Instant) -> Result<Vec<u8>, Error> {
        let mut header = [0; 2];
        self.read_exact(&mut header, started, "read response header")?;
        if header[0] != ADDRESS {
            return Err(Error::InvalidAddress(header[0]));
        }
        let length = usize::from(header[1]);
        if length < 4 {
            return Err(Error::InvalidLength(length));
        }
        let mut response = vec![0; length];
        response[..2].copy_from_slice(&header);
        self.read_exact(&mut response[2..], started, "read response body")?;
        if response.iter().fold(0, |sum, byte| sum ^ byte) != 0 {
            return Err(Error::InvalidChecksum);
        }
        Ok(response)
    }

    fn read_exact(
        &mut self,
        mut output: &mut [u8],
        started: Instant,
        stage: &'static str,
    ) -> Result<(), Error> {
        while !output.is_empty() {
            self.remaining_timeout(started, stage)?;
            match self.transport.read(output) {
                Ok(0) => {
                    return Err(Error::io(
                        stage,
                        io::Error::from(io::ErrorKind::UnexpectedEof),
                    ));
                }
                Ok(count) => output = &mut output[count..],
                Err(error) if error.kind() == io::ErrorKind::Interrupted => continue,
                Err(error) => return Err(Error::io(stage, error)),
            }
        }
        Ok(())
    }

    fn remaining_timeout(&mut self, started: Instant, stage: &'static str) -> Result<(), Error> {
        let remaining = self
            .timeout
            .checked_sub(started.elapsed())
            .filter(|d| !d.is_zero())
            .ok_or(Error::Timeout { stage })?;
        self.configure_timeout(remaining)
    }

    fn configure_timeout(&mut self, timeout: Duration) -> Result<(), Error> {
        self.transport
            .set_timeout(timeout)
            .map_err(|source| Error::TransportSetup {
                stage: "configure timeout",
                source,
            })
    }
}

fn encode(payload: &[u8]) -> Result<Vec<u8>, Error> {
    let length = payload
        .len()
        .checked_add(3)
        .ok_or(Error::InvalidLength(payload.len()))?;
    let length_byte = u8::try_from(length).map_err(|_| Error::InvalidLength(length))?;
    let mut packet = vec![ADDRESS, length_byte];
    packet.extend_from_slice(payload);
    packet.push(packet.iter().fold(0, |sum, byte| sum ^ byte));
    Ok(packet)
}
