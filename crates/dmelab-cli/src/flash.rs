use std::fs::{self, File, OpenOptions};
use std::io::{Read, Write};
use std::path::PathBuf;

use anyhow::{Context, Result};
use clap::{Args, ValueEnum};
use dmelab_ms43::{BaudRate, FlashMode, FlashProgress, validate_image};

use crate::connection::ConnectionArgs;

#[derive(Clone, Copy, ValueEnum)]
enum Mode {
    /// Full 512 KiB read; write program and calibration without bootloader.
    Full,
    /// Read/write calibration only (64 KiB, or calibration from a full input).
    Partial,
}

impl Mode {
    fn flash_mode(self) -> FlashMode {
        match self {
            Self::Full => FlashMode::Full,
            Self::Partial => FlashMode::Calibration,
        }
    }
}

#[derive(Args)]
struct TransferArgs {
    #[command(flatten)]
    connection: ConnectionArgs,
    /// Transfer speed: 19200, 38400, 50000, 107000, 125000. Restores 9600 on completion.
    #[arg(long, default_value = "125000", value_parser = transfer_speed)]
    baud_rate: BaudRate,
    #[arg(long, value_enum, default_value = "full")]
    mode: Mode,
}

#[derive(Args)]
pub(crate) struct ReadArgs {
    #[command(flatten)]
    transfer: TransferArgs,
    /// New output .bin file; existing files are never overwritten.
    #[arg(long)]
    output: PathBuf,
}

#[derive(Args)]
pub(crate) struct WriteArgs {
    #[command(flatten)]
    transfer: TransferArgs,
    /// MS43 .bin image: 512 KiB full, or 64 KiB for partial mode.
    #[arg(long)]
    input: PathBuf,
}

fn transfer_speed(value: &str) -> Result<BaudRate, String> {
    let speed = value
        .parse::<BaudRate>()
        .map_err(|error| error.to_string())?;
    if speed == BaudRate::B9600 {
        return Err("read/write operations require a speed above 9600 baud".into());
    }
    Ok(speed)
}

pub(crate) fn read(arguments: ReadArgs) -> Result<()> {
    // Reserve the output before touching hardware. A failed operation removes
    // this newly created file so it cannot be mistaken for a complete dump.
    let mut output = OpenOptions::new()
        .write(true)
        .create_new(true)
        .open(&arguments.output)
        .with_context(|| format!("Cannot create new output {}", arguments.output.display()))?;
    let result = (|| -> Result<usize> {
        let (mut session, port) = arguments.transfer.connection.open()?;
        let image = session
            .read_flash(
                arguments.transfer.mode.flash_mode(),
                arguments.transfer.baud_rate,
                &mut progress_reporter(),
            )
            .with_context(|| format!("Failed to read MS43 at {port}"))?;
        output
            .write_all(&image)
            .context("Failed to save the complete flash image")?;
        output
            .sync_all()
            .context("Failed to sync the flash image")?;
        Ok(image.len())
    })();
    drop(output);
    match result {
        Ok(length) => {
            println!("Saved {length} bytes to {}", arguments.output.display());
            Ok(())
        }
        Err(error) => {
            if let Err(cleanup) = fs::remove_file(&arguments.output) {
                return Err(error.context(format!(
                    "Failed to remove incomplete output {}: {cleanup}",
                    arguments.output.display()
                )));
            }
            Err(error)
        }
    }
}

pub(crate) fn write(arguments: WriteArgs) -> Result<()> {
    let mut image = Vec::new();
    File::open(&arguments.input)
        .with_context(|| format!("Cannot open input {}", arguments.input.display()))?
        .take(0x80001)
        .read_to_end(&mut image)
        .context("Failed to read input image")?;
    validate_image(&image, arguments.transfer.mode.flash_mode())
        .context("Input image rejected before opening the adapter")?;
    let (mut session, port) = arguments.transfer.connection.open()?;
    session
        .write_flash(
            arguments.transfer.mode.flash_mode(),
            &image,
            arguments.transfer.baud_rate,
            &mut progress_reporter(),
        )
        .with_context(|| format!("Failed to write MS43 at {port}"))?;
    let sections = match arguments.transfer.mode {
        Mode::Full => "Program and calibration",
        Mode::Partial => "Calibration",
    };
    println!("{sections} write completed; bootloader was not written.");
    println!("Switch ignition off, wait 10 seconds, then turn the key to position 2.");
    Ok(())
}

fn progress_reporter() -> impl FnMut(FlashProgress) {
    let mut previous = None;
    move |progress: FlashProgress| {
        let percent = progress
            .completed
            .saturating_mul(100)
            .checked_div(progress.total)
            .unwrap_or(0);
        let current = (progress.stage, percent);
        if previous != Some(current) {
            eprintln!("{}: {percent}%", progress.stage);
            previous = Some(current);
        }
    }
}
