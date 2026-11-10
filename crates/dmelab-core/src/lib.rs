//! Platform-independent contracts for synchronous DME communication.

use std::io::{self, Read, Write};
use std::time::Duration;

/// A blocking byte stream owned by one session.
///
/// Reads and writes must honor the configured timeout. Implementations must not
/// create threads. Dropping the transport releases the connection.
pub trait Transport: Read + Write {
    /// Set the timeout for subsequent reads and writes; zero is invalid.
    fn set_timeout(&mut self, timeout: Duration) -> io::Result<()>;

    /// Change UART speed after the protocol has negotiated it with the ECU.
    /// This operation must preserve the other line settings.
    fn set_baud_rate(&mut self, baud_rate: u32) -> io::Result<()>;

    /// Discard stale input and pending output before starting a transaction.
    fn clear_buffers(&mut self) -> io::Result<()>;
}
