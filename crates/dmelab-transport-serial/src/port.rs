use std::io::{self, Read, Write};
use std::time::Duration;

use dmelab_core::Transport;
use serialport::{ClearBuffer, DataBits, FlowControl, Parity, SerialPort, StopBits};

#[derive(Debug, thiserror::Error)]
pub enum Error {
    #[error("serial timeout must be greater than zero")]
    InvalidTimeout,
    #[error("cannot open serial port {path}")]
    Open {
        path: String,
        #[source]
        source: serialport::Error,
    },
    #[error("cannot configure serial port {path}")]
    Configure {
        path: String,
        #[source]
        source: serialport::Error,
    },
    #[error("cannot enumerate serial ports")]
    Enumerate(#[source] serialport::Error),
}

/// Port metadata is informational: adapters are never rejected by USB vendor ID.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct PortInfo {
    pub name: String,
    pub usb_vid: Option<u16>,
    pub usb_pid: Option<u16>,
    pub serial_number: Option<String>,
    pub description: Option<String>,
}

pub fn list_ports() -> Result<Vec<PortInfo>, Error> {
    let mut ports = Vec::new();
    for port in serialport::available_ports().map_err(Error::Enumerate)? {
        let mut info = PortInfo {
            name: port.port_name,
            usb_vid: None,
            usb_pid: None,
            serial_number: None,
            description: None,
        };
        if let serialport::SerialPortType::UsbPort(usb) = port.port_type {
            info.usb_vid = Some(usb.vid);
            info.usb_pid = Some(usb.pid);
            info.serial_number = usb.serial_number;
            info.description = usb.product.or(usb.manufacturer);
        }
        ports.push(info);
    }
    ports.sort_by(|left, right| left.name.cmp(&right.name));
    Ok(ports)
}

/// Owns an OS serial handle. Opening configures 9600 baud, 8E1, no flow control.
/// Reads and writes execute in the calling thread; no worker is created.
pub struct SerialTransport {
    port: Box<dyn SerialPort>,
}

impl SerialTransport {
    pub fn open(path: &str, timeout: Duration) -> Result<Self, Error> {
        if timeout.is_zero() {
            return Err(Error::InvalidTimeout);
        }
        let mut port = serialport::new(path, 9600)
            .data_bits(DataBits::Eight)
            .parity(Parity::Even)
            .stop_bits(StopBits::One)
            .flow_control(FlowControl::None)
            .timeout(timeout)
            .dtr_on_open(false)
            .open()
            .map_err(|source| Error::Open {
                path: path.to_owned(),
                source,
            })?;
        port.write_request_to_send(false)
            .and_then(|()| port.write_data_terminal_ready(false))
            .map_err(|source| Error::Configure {
                path: path.to_owned(),
                source,
            })?;
        Ok(Self { port })
    }

    /// Reset the already-owned handle before a new diagnostic operation.
    /// This only configures the UART; it does not negotiate the ECU's speed.
    pub fn configure_initial(&mut self) -> io::Result<()> {
        self.port.set_baud_rate(9600)?;
        self.port.set_data_bits(DataBits::Eight)?;
        self.port.set_parity(Parity::Even)?;
        self.port.set_stop_bits(StopBits::One)?;
        self.port.set_flow_control(FlowControl::None)?;
        self.port.write_request_to_send(false)?;
        self.port.write_data_terminal_ready(false)?;
        Ok(())
    }
}

impl Read for SerialTransport {
    fn read(&mut self, buffer: &mut [u8]) -> io::Result<usize> {
        self.port.read(buffer)
    }
}

impl Write for SerialTransport {
    fn write(&mut self, buffer: &[u8]) -> io::Result<usize> {
        self.port.write(buffer)
    }

    fn flush(&mut self) -> io::Result<()> {
        self.port.flush()
    }
}

impl Transport for SerialTransport {
    fn set_baud_rate(&mut self, baud_rate: u32) -> io::Result<()> {
        self.port.set_baud_rate(baud_rate).map_err(io::Error::from)
    }

    fn set_timeout(&mut self, timeout: Duration) -> io::Result<()> {
        if timeout.is_zero() {
            return Err(io::Error::new(
                io::ErrorKind::InvalidInput,
                "serial timeout must be greater than zero",
            ));
        }
        self.port.set_timeout(timeout).map_err(io::Error::from)
    }

    fn clear_buffers(&mut self) -> io::Result<()> {
        self.port.clear(ClearBuffer::All).map_err(io::Error::from)
    }
}
