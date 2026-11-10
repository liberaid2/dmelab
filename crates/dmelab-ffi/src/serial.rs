//! Native desktop serial bridge. Shared ownership is local to one Ms43Client.
//! Keep io::Error kinds intact for the protocol's retry/disconnection decisions.

use std::error::Error as _;
use std::io::{self, Read, Write};
use std::sync::{Arc, Mutex, MutexGuard};
use std::time::Duration;

use dmelab_core::Transport;
use dmelab_transport_serial::SerialTransport;

use crate::DmeError;

/// Informational metadata; a missing VID/PID does not prohibit opening a port.
#[derive(Debug, Clone, uniffi::Record)]
pub struct SerialPortInfo {
    pub name: String,
    pub usb_vid: Option<u16>,
    pub usb_pid: Option<u16>,
    pub serial_number: Option<String>,
    pub description: Option<String>,
}

#[uniffi::export]
pub fn list_serial_ports() -> Result<Vec<SerialPortInfo>, DmeError> {
    let ports = dmelab_transport_serial::list_ports().map_err(serial_error)?;
    Ok(ports
        .into_iter()
        .map(|port| SerialPortInfo {
            name: port.name,
            usb_vid: port.usb_vid,
            usb_pid: port.usb_pid,
            serial_number: port.serial_number,
            description: port.description,
        })
        .collect())
}

// Arc keeps the OS handle alive for an operation-scoped Session. Mutex gives
// UniFFI's Send + Sync object safe ownership; the client lock serializes commands.
// The wrapper creates no thread and never converts native errors to callbacks.
#[derive(Clone)]
pub(super) struct SharedSerialTransport {
    port: Arc<Mutex<SerialTransport>>,
}

impl SharedSerialTransport {
    pub(super) fn open(path: &str, timeout: Duration) -> Result<Self, DmeError> {
        let port = SerialTransport::open(path, timeout).map_err(serial_error)?;
        Ok(Self {
            port: Arc::new(Mutex::new(port)),
        })
    }

    pub(super) fn configure_initial(&self) -> io::Result<()> {
        self.lock()?.configure_initial()
    }

    fn lock(&self) -> io::Result<MutexGuard<'_, SerialTransport>> {
        self.port
            .lock()
            .map_err(|_| io::Error::other("serial handle lock is poisoned; reopen the connection"))
    }
}

impl Read for SharedSerialTransport {
    fn read(&mut self, buffer: &mut [u8]) -> io::Result<usize> {
        self.lock()?.read(buffer)
    }
}

impl Write for SharedSerialTransport {
    fn write(&mut self, bytes: &[u8]) -> io::Result<usize> {
        self.lock()?.write(bytes)
    }
    fn flush(&mut self) -> io::Result<()> {
        self.lock()?.flush()
    }
}

impl Transport for SharedSerialTransport {
    fn set_timeout(&mut self, timeout: Duration) -> io::Result<()> {
        self.lock()?.set_timeout(timeout)
    }
    fn set_baud_rate(&mut self, baud_rate: u32) -> io::Result<()> {
        self.lock()?.set_baud_rate(baud_rate)
    }
    fn clear_buffers(&mut self) -> io::Result<()> {
        self.lock()?.clear_buffers()
    }
}

fn serial_error(error: dmelab_transport_serial::Error) -> DmeError {
    let details = match error.source() {
        Some(source) => format!("{error}: {source}"),
        None => error.to_string(),
    };
    DmeError::Transport { details }
}
