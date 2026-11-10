//! Synchronous cross-platform serial transport for an MS43 K-line cable.

mod port;

pub use port::{Error, PortInfo, SerialTransport, list_ports};
