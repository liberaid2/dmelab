use std::time::Duration;

use anyhow::{Context, Result};
use clap::Args;
use dmelab_ms43::Session;
use dmelab_transport_serial::SerialTransport;

#[derive(Args)]
pub(crate) struct ConnectionArgs {
    /// Serial device path; defaults to the only available macOS usbserial device.
    #[arg(long)]
    pub port: Option<String>,
    /// Diagnostics, identification and speed negotiation transaction timeout, in milliseconds.
    #[arg(long, default_value_t = 1000, value_parser = clap::value_parser!(u64).range(1..=60_000))]
    pub timeout_ms: u64,
}

impl ConnectionArgs {
    pub fn open(self) -> Result<(Session, String)> {
        let port = crate::port_selection::select_port(self.port)?;
        let timeout = Duration::from_millis(self.timeout_ms);
        let transport = SerialTransport::open(&port, timeout)
            .with_context(|| format!("Failed to open K-line adapter at {port}"))?;
        let session = Session::new(transport, timeout).context("Failed to create MS43 session")?;
        Ok((session, port))
    }
}
