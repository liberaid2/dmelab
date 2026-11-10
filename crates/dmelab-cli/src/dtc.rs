use anyhow::{Context, Result};

use crate::connection::ConnectionArgs;

pub(crate) fn clear(connection: ConnectionArgs) -> Result<()> {
    let (mut session, port) = connection.open()?;
    session
        .clear_dtcs()
        .with_context(|| format!("Failed to confirm MS43 DTC clear at {port}"))?;
    println!("ECU acknowledged DTC memory clear.");
    println!("Run 'dmelab dtc' to read the current fault list; recurring faults may reappear.");
    Ok(())
}

pub(crate) fn read(connection: ConnectionArgs) -> Result<()> {
    let (mut session, port) = connection.open()?;
    let dtcs = session
        .read_dtcs()
        .with_context(|| format!("Failed to read MS43 DTCs at {port}"))?;
    if dtcs.is_empty() {
        println!("No DTCs found.");
        return Ok(());
    }
    println!("MS43 DTCs: {}", dtcs.len());
    for dtc in dtcs {
        println!(
            "Code: {} (0x{:02X}), frequency: {}, data: {:02X?}, raw: {:02X?}",
            dtc.code, dtc.code, dtc.frequency, dtc.data, dtc.raw,
        );
    }
    Ok(())
}
