use std::io::{self, Read, Write};
use std::sync::Arc;
use std::time::Duration;

use crate::TransportFailure;

/// The app owns an open USB connection. All callbacks run synchronously on the
/// thread calling Ms43Client. Timeouts must bound each callback's blocking work.
#[uniffi::export(with_foreign)]
pub trait KlineTransport: Send + Sync {
    /// Set 9600 baud, 8E1, no flow control, RTS/DTR off on the already open port.
    fn configure_initial(&self) -> Result<(), TransportFailure>;
    fn read(&self, max_bytes: u32, timeout_ms: u32) -> Result<Vec<u8>, TransportFailure>;
    fn write(&self, data: Vec<u8>, timeout_ms: u32) -> Result<u32, TransportFailure>;
    fn set_baud_rate(&self, baud_rate: u32) -> Result<(), TransportFailure>;
    fn clear_buffers(&self) -> Result<(), TransportFailure>;
}

pub(crate) struct CallbackTransport {
    callback: Arc<dyn KlineTransport>,
    timeout_ms: u32,
}

impl CallbackTransport {
    pub fn new(callback: Arc<dyn KlineTransport>, timeout_ms: u32) -> Self {
        Self {
            callback,
            timeout_ms,
        }
    }
}

impl Read for CallbackTransport {
    fn read(&mut self, buffer: &mut [u8]) -> io::Result<usize> {
        if buffer.is_empty() {
            return Ok(0);
        }
        let data = self.callback.read(buffer.len() as u32, self.timeout_ms)?;
        if data.len() > buffer.len() {
            return Err(io::Error::new(
                io::ErrorKind::InvalidData,
                "transport read exceeded requested buffer",
            ));
        }
        if data.is_empty() {
            return Err(io::Error::from(io::ErrorKind::TimedOut));
        }
        buffer[..data.len()].copy_from_slice(&data);
        Ok(data.len())
    }
}

impl Write for CallbackTransport {
    fn write(&mut self, bytes: &[u8]) -> io::Result<usize> {
        let count = self.callback.write(bytes.to_vec(), self.timeout_ms)? as usize;
        if count > bytes.len() {
            return Err(io::Error::new(
                io::ErrorKind::InvalidData,
                "transport write exceeded supplied buffer",
            ));
        }
        Ok(count)
    }

    fn flush(&mut self) -> io::Result<()> {
        Ok(())
    }
}

impl dmelab_core::Transport for CallbackTransport {
    fn set_timeout(&mut self, timeout: Duration) -> io::Result<()> {
        if timeout.is_zero() || timeout > Duration::from_secs(60) {
            return Err(io::Error::new(
                io::ErrorKind::InvalidInput,
                "invalid callback timeout",
            ));
        }
        self.timeout_ms = timeout.as_millis().max(1) as u32;
        Ok(())
    }

    fn set_baud_rate(&mut self, baud_rate: u32) -> io::Result<()> {
        self.callback.set_baud_rate(baud_rate).map_err(Into::into)
    }

    fn clear_buffers(&mut self) -> io::Result<()> {
        self.callback.clear_buffers().map_err(Into::into)
    }
}
