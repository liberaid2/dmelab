use std::io::{self, Write};
use std::ops::ControlFlow;
use std::time::Instant;

use anyhow::{Context, Result};
use clap::{Args, ValueEnum};
use dmelab_ms43::{
    LiveDataSelection, LiveMeasurements, SelectedLiveData, SelectedLiveDataEvent, Session,
};

use crate::connection::ConnectionArgs;

#[derive(Clone, Copy, PartialEq, Eq, ValueEnum)]
enum LiveBlock {
    /// Main measurements (0B 03), including actual camshaft angles.
    #[value(alias = "03")]
    Measurements,
    /// Actual, target and reference VANOS positions (0B 90).
    #[value(alias = "90")]
    Vanos,
    /// Digital state / DISA command (0B 04).
    #[value(alias = "04")]
    Digital,
    /// Pedal/throttle channels and adaptations (0B 92).
    #[value(alias = "92")]
    Pedal,
    /// Cylinder roughness and misfire counters (2B 05).
    #[value(alias = "2b05")]
    RoughRunning,
    /// All five supported blocks.
    All,
}

#[derive(Args)]
pub(crate) struct LiveArgs {
    #[command(flatten)]
    connection: ConnectionArgs,
    /// Blocks to read, comma-separated. Any nonempty combination is supported.
    #[arg(long, value_enum, value_delimiter = ',', num_args = 1..,
          default_value = "measurements,vanos,digital")]
    blocks: Vec<LiveBlock>,
    /// Poll continuously on one session; packet errors are reported and retried.
    #[arg(long)]
    watch: bool,
    /// Stop after this many complete samples; requires --watch.
    #[arg(long, requires = "watch", value_parser = clap::value_parser!(u64).range(1..))]
    samples: Option<u64>,
}

pub(crate) fn read(arguments: LiveArgs) -> Result<()> {
    let all = arguments.blocks.contains(&LiveBlock::All);
    let selection = LiveDataSelection {
        measurements: all || arguments.blocks.contains(&LiveBlock::Measurements),
        vanos: all || arguments.blocks.contains(&LiveBlock::Vanos),
        digital: all || arguments.blocks.contains(&LiveBlock::Digital),
        pedal: all || arguments.blocks.contains(&LiveBlock::Pedal),
        rough_running: all || arguments.blocks.contains(&LiveBlock::RoughRunning),
    };
    selection.validate()?;
    let (mut session, port) = arguments.connection.open()?;
    let stdout = io::stdout();
    let mut output = stdout.lock();
    if arguments.watch {
        return watch(&mut session, selection, arguments.samples, &mut output)
            .with_context(|| format!("Failed to stream MS43 live data at {port}"));
    }
    let data = session
        .read_live_data_selected(selection)
        .with_context(|| format!("Failed to read MS43 live data at {port}"))?;
    print_data(&mut output, &data)?;
    Ok(())
}

fn watch(
    session: &mut Session,
    selection: LiveDataSelection,
    limit: Option<u64>,
    output: &mut impl Write,
) -> Result<()> {
    let started = Instant::now();
    let mut samples = 0_u64;
    let mut output_error = None;
    session.stream_live_data_selected(selection, &|| false, &mut |event| {
        match event {
            SelectedLiveDataEvent::Sample(data) => {
                samples = samples.saturating_add(1);
                let result = writeln!(
                    output,
                    "Sample {samples}, elapsed {:.3} s",
                    started.elapsed().as_secs_f64()
                )
                .and_then(|()| print_data(output, &data))
                .and_then(|()| output.flush());
                if let Err(error) = result {
                    output_error = Some(error);
                    return ControlFlow::Break(());
                }
                if limit.is_some_and(|limit| samples >= limit) {
                    return ControlFlow::Break(());
                }
            }
            SelectedLiveDataEvent::Recovering {
                consecutive_failures,
                error,
            } => {
                eprintln!("Live data retry {consecutive_failures}: {error}");
            }
        }
        ControlFlow::Continue(())
    })?;
    if let Some(error) = output_error {
        return Err(error).context("Failed to write live data output");
    }
    Ok(())
}

// Single reads and streams publish only the blocks actually requested.
fn print_data(output: &mut impl Write, data: &SelectedLiveData) -> io::Result<()> {
    if let Some(measurements) = &data.measurements {
        print_measurements(output, measurements)?;
    }
    if let Some(vanos) = &data.vanos {
        for (name, position) in [("Intake", vanos.intake), ("Exhaust", vanos.exhaust)] {
            writeln!(
                output,
                "{name} VANOS: actual={:.3}, target={:.3}, reference={:.3} deg crankshaft",
                position.actual_deg, position.target_deg, position.reference_deg
            )?;
        }
    }
    if let Some(digital) = &data.digital {
        writeln!(
            output,
            "DISA command: {}",
            if digital.disa_command_active {
                "active"
            } else {
                "inactive"
            }
        )?;
    }
    if let Some(pedal) = &data.pedal {
        crate::live_extended::print_pedal(output, pedal)?;
    }
    if let Some(rough_running) = &data.rough_running {
        crate::live_extended::print_rough_running(output, rough_running)?;
    }
    Ok(())
}

// All 28 values form one coherent 0B 03 block; keep their formatter together.
fn print_measurements(output: &mut impl Write, data: &LiveMeasurements) -> io::Result<()> {
    writeln!(output, "Engine speed: {} rpm", data.engine_speed_rpm)?;
    writeln!(output, "Vehicle speed: {} km/h", data.vehicle_speed_kmh)?;
    writeln!(
        output,
        "Accelerator pedal angle: {:.2} deg",
        data.accelerator_pedal_angle_deg
    )?;
    writeln!(output, "Throttle angle: {:.2} deg", data.throttle_angle_deg)?;
    writeln!(output, "Mass airflow: {:.2} kg/h", data.mass_airflow_kgh)?;
    writeln!(
        output,
        "Intake air temperature: {:.2} C",
        data.intake_air_temperature_c
    )?;
    writeln!(
        output,
        "Coolant temperature: {:.2} C",
        data.coolant_temperature_c
    )?;
    writeln!(output, "Oil temperature: {:.2} C", data.oil_temperature_c)?;
    writeln!(
        output,
        "Radiator outlet temperature: {:.2} C",
        data.radiator_outlet_temperature_c
    )?;
    writeln!(
        output,
        "Cylinder 1 ignition timing: {:.2} deg crankshaft",
        data.ignition_timing_cylinder_1_deg
    )?;
    writeln!(
        output,
        "Bank 1 injection time: {:.3} ms",
        data.injection_time_bank_1_ms
    )?;
    writeln!(
        output,
        "Idle speed control: integrator={:.3} %, valve duty={:.3} %",
        data.idle_speed_integrator_pct, data.idle_air_control_duty_pct
    )?;
    writeln!(
        output,
        "Camshaft angles: intake={:.3}, exhaust={:.3} deg crankshaft",
        data.intake_camshaft_angle_deg, data.exhaust_camshaft_angle_deg
    )?;
    writeln!(
        output,
        "Supply voltage: terminal 15={:.3} V, battery={:.3} V",
        data.terminal_15_voltage_v, data.battery_voltage_v
    )?;
    writeln!(
        output,
        "Bank 1 lambda correction factor: {:.5}",
        data.lambda_integrator_bank_1
    )?;
    writeln!(
        output,
        "Bank 2 lambda correction factor: {:.5}",
        data.lambda_integrator_bank_2
    )?;
    writeln!(
        output,
        "Upstream oxygen sensor heater duty: bank 1={:.3} %, bank 2={:.3} %",
        data.oxygen_sensor_heater_upstream_bank_1_duty_pct,
        data.oxygen_sensor_heater_upstream_bank_2_duty_pct
    )?;
    writeln!(
        output,
        "Downstream oxygen sensor heater duty: bank 1={:.3} %, bank 2={:.3} %",
        data.oxygen_sensor_heater_downstream_bank_1_duty_pct,
        data.oxygen_sensor_heater_downstream_bank_2_duty_pct
    )?;
    writeln!(
        output,
        "Air mass per stroke: {:.2} mg/stroke",
        data.air_mass_per_stroke_mg
    )?;
    writeln!(
        output,
        "Ambient pressure: {:.3} hPa",
        data.ambient_pressure_hpa
    )?;
    writeln!(
        output,
        "Cylinder 2 mean knock signal: {:.4} V",
        data.knock_signal_cylinder_2_v
    )?;
    writeln!(
        output,
        "Cylinder 5 mean knock signal: {:.4} V",
        data.knock_signal_cylinder_5_v
    )?;
    writeln!(
        output,
        "Cooling fan command: {:.2} %",
        data.cooling_fan_duty_pct
    )?;
    Ok(())
}
