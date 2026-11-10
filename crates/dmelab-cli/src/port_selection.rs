use anyhow::{Context, Result, bail};
use dmelab_transport_serial::list_ports;

pub(crate) fn select_port(explicit: Option<String>) -> Result<String> {
    if let Some(port) = explicit {
        return Ok(port);
    }

    let ports = list_ports().context("Failed to enumerate ports for automatic selection")?;
    let mut candidates = Vec::new();
    for port in &ports {
        let name = &port.name;
        if name.starts_with("/dev/cu.usbserial") {
            candidates.push(name.clone());
        } else if let Some(suffix) = name.strip_prefix("/dev/tty.usbserial") {
            // macOS can enumerate both names for the same device. Prefer cu.
            let callout = format!("/dev/cu.usbserial{suffix}");
            if !ports.iter().any(|other| other.name == callout) {
                candidates.push(name.clone());
            }
        }
    }
    candidates.sort();
    candidates.dedup();
    match candidates.as_slice() {
        [] => bail!(
            "No usbserial port found; connect the adapter or specify --port PATH (see `dmelab ports`)"
        ),
        [port] => {
            eprintln!("Using serial port: {port}");
            Ok(port.clone())
        }
        _ => bail!(
            "Multiple usbserial ports found: {}; specify --port PATH",
            candidates.join(", ")
        ),
    }
}
