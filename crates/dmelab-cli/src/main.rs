use anyhow::{Context, Result};
use clap::{Parser, Subcommand};
use dmelab_ms43::{BaudRate, Identification, Session};
use dmelab_transport_serial::list_ports;

mod actuators;
mod connection;
mod dtc;
mod flash;
mod live_data;
mod live_extended;
mod port_selection;

#[derive(Parser)]
#[command(name = "dmelab", version, about = "Synchronous MS43 DME diagnostics")]
struct Arguments {
    /// Include protocol debug logs on stderr.
    #[arg(short, long, global = true)]
    verbose: bool,
    #[command(subcommand)]
    command: Command,
}

#[derive(Subcommand)]
enum Command {
    /// List serial ports and available USB adapter metadata.
    Ports,
    /// Read engine measurements, VANOS and DISA at 9600 baud, optionally continuously.
    Live(live_data::LiveArgs),
    /// Command DISA actuation once; diagnostic-end is a separate operation.
    TestDisa {
        #[command(flatten)]
        connection: connection::ConnectionArgs,
        #[arg(long, value_enum)]
        state: actuators::DisaState,
    },
    /// Command electric fan duty once, in percent; not an RPM setting.
    TestFan {
        #[command(flatten)]
        connection: connection::ConnectionArgs,
        #[arg(long, allow_hyphen_values = true)]
        duty_percent: f64,
    },
    /// End diagnostic actuator control once, requiring an ECU acknowledgement.
    DiagnosticEnd {
        #[command(flatten)]
        connection: connection::ConnectionArgs,
    },
    /// Read MS43 diagnostic trouble codes at 9600 baud.
    Dtc {
        #[command(flatten)]
        connection: connection::ConnectionArgs,
    },
    /// Clear MS43 DTC memory once at 9600 baud; require an ECU acknowledgement.
    ClearDtc {
        #[command(flatten)]
        connection: connection::ConnectionArgs,
    },
    /// Read full flash or calibration at a negotiated higher speed.
    Read(flash::ReadArgs),
    /// Write program/calibration or calibration only; never writes bootloader.
    Write(flash::WriteArgs),
    /// Identify MS43, optionally negotiating a faster K-line speed.
    Identify {
        #[command(flatten)]
        connection: connection::ConnectionArgs,
        /// Identify at this speed after initial MS43 detection at 9600; restore 9600 before exit.
        /// Supported: 9600, 19200, 38400, 50000, 107000, 125000.
        #[arg(long, default_value = "9600")]
        baud_rate: BaudRate,
    },
}

fn main() -> Result<()> {
    let arguments = Arguments::parse();
    tracing_subscriber::fmt()
        .with_max_level(if arguments.verbose {
            tracing::Level::DEBUG
        } else {
            tracing::Level::WARN
        })
        .with_writer(std::io::stderr)
        .with_ansi(false)
        .without_time()
        .init();

    match arguments.command {
        Command::Ports => print_ports(),
        Command::Live(arguments) => live_data::read(arguments),
        Command::TestDisa { connection, state } => actuators::disa(connection, state),
        Command::TestFan {
            connection,
            duty_percent,
        } => actuators::fan(connection, duty_percent),
        Command::DiagnosticEnd { connection } => actuators::end(connection),
        Command::Dtc { connection } => dtc::read(connection),
        Command::ClearDtc { connection } => dtc::clear(connection),
        Command::Read(arguments) => flash::read(arguments),
        Command::Write(arguments) => flash::write(arguments),
        Command::Identify {
            connection,
            baud_rate,
        } => identify(connection, baud_rate),
    }
}

fn print_ports() -> Result<()> {
    let ports = list_ports().context("Failed to list serial ports")?;
    if ports.is_empty() {
        println!("No serial ports found.");
    }
    for port in ports {
        print!("{}", port.name);
        if let (Some(vid), Some(pid)) = (port.usb_vid, port.usb_pid) {
            print!("\tUSB {vid:04x}:{pid:04x}");
        }
        if let Some(serial) = port.serial_number {
            print!("\tserial={serial}");
        }
        if let Some(description) = port.description {
            print!("\t{description}");
        }
        println!();
    }
    Ok(())
}

fn identify(connection: connection::ConnectionArgs, baud_rate: BaudRate) -> Result<()> {
    let (mut session, port) = connection.open()?;
    let identification = identify_at_speed(&mut session, baud_rate)
        .with_context(|| format!("Failed to identify MS43 at {port}"))?;
    println!("ECU: MS43");
    println!("Hardware ID: {}", identification.hardware_id);
    println!("Flash identifier: {}", identification.flash_identifier);
    println!("Firmware version: {}", identification.firmware_version);
    if baud_rate != BaudRate::B9600 {
        println!(
            "Identification baud rate: {} (restored 9600)",
            baud_rate.as_u32()
        );
    }
    Ok(())
}

fn identify_at_speed(session: &mut Session, baud_rate: BaudRate) -> Result<Identification> {
    let initial = session
        .identify()
        .context("Initial MS43 detection at 9600 baud failed")?;
    if baud_rate == BaudRate::B9600 {
        return Ok(initial);
    }
    let operation = session
        .change_baud_rate(baud_rate)
        .and_then(|()| session.identify());
    let restore = session.restore_default_baud_rate();
    match (operation, restore) {
        (Ok(identification), Ok(())) => Ok(identification),
        (Err(error), Ok(())) => Err(error.into()),
        (Ok(_), Err(error)) => Err(error).context("Failed to restore 9600 baud"),
        (Err(error), Err(restore_error)) => {
            Err(error).with_context(|| format!("Restore to 9600 baud also failed: {restore_error}"))
        }
    }
}
