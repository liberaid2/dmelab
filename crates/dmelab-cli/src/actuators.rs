use anyhow::{Context, Result};
use clap::ValueEnum;

use crate::connection::ConnectionArgs;

#[derive(Clone, Copy, ValueEnum)]
pub(crate) enum DisaState {
    On,
    Off,
}

pub(crate) fn disa(connection: ConnectionArgs, state: DisaState) -> Result<()> {
    let enabled = matches!(state, DisaState::On);
    let (mut session, port) = connection.open()?;
    session
        .set_disa(enabled)
        .with_context(|| format!("Failed to command DISA at {port}"))?;
    println!(
        "DISA actuation {}: acknowledged by ECU",
        if enabled { "on" } else { "off" }
    );
    println!("To end diagnostic control: dmelab diagnostic-end --port {port}");
    Ok(())
}

pub(crate) fn fan(connection: ConnectionArgs, duty_pct: f64) -> Result<()> {
    dmelab_ms43::validate_cooling_fan_duty(duty_pct)?;
    let (mut session, port) = connection.open()?;
    session
        .set_cooling_fan(duty_pct)
        .with_context(|| format!("Failed to command cooling fan at {port}"))?;
    println!("Cooling fan duty {duty_pct:.2}%: acknowledged by ECU");
    println!("To end diagnostic control: dmelab diagnostic-end --port {port}");
    Ok(())
}

pub(crate) fn end(connection: ConnectionArgs) -> Result<()> {
    let (mut session, port) = connection.open()?;
    session
        .end_diagnostic_control()
        .with_context(|| format!("Failed to end diagnostic control at {port}"))?;
    println!("End diagnostic control: acknowledged by ECU");
    Ok(())
}
