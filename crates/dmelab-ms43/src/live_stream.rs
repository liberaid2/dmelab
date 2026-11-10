//! Blocking, cancellable polling with one identified session and packet recovery.

use std::io::ErrorKind;
use std::ops::ControlFlow;
use std::time::Duration;

use crate::session::LinkState;
use crate::{BaudRate, Error, LiveData, LiveDataSelection, SelectedLiveData, Session};

#[cfg(test)]
mod tests;

/// Failed samples are reported separately and never published as measurements.
#[derive(Debug)]
pub enum LiveDataEvent {
    // LiveData now contains all measurement fields; keep the event enum compact.
    Sample(Box<LiveData>),
    Recovering {
        consecutive_failures: u64,
        error: Error,
    },
}

/// A sample contains exactly the requested blocks, or a recoverable failure.
#[derive(Debug)]
pub enum SelectedLiveDataEvent {
    Sample(Box<SelectedLiveData>),
    Recovering {
        consecutive_failures: u64,
        error: Error,
    },
}

impl Session {
    /// Poll at 9600 until cancelled, the callback breaks, or the transport is lost.
    /// Packet errors are retried without a failure-count limit, with capped backoff.
    /// Both callbacks run synchronously; no threads or background work are created.
    pub fn stream_live_data(
        &mut self,
        should_stop: &dyn Fn() -> bool,
        on_event: &mut dyn FnMut(LiveDataEvent) -> ControlFlow<()>,
    ) -> Result<(), Error> {
        let mut conversion_error = None;
        self.stream_live_data_selected(LiveDataSelection::default(), should_stop, &mut |event| {
            let event = match event {
                SelectedLiveDataEvent::Sample(data) => match (*data).try_into() {
                    Ok(data) => LiveDataEvent::Sample(Box::new(data)),
                    Err(error) => {
                        conversion_error = Some(error);
                        return ControlFlow::Break(());
                    }
                },
                SelectedLiveDataEvent::Recovering {
                    consecutive_failures,
                    error,
                } => LiveDataEvent::Recovering {
                    consecutive_failures,
                    error,
                },
            };
            on_event(event)
        })?;
        match conversion_error {
            Some(error) => Err(error),
            None => Ok(()),
        }
    }

    /// Same recovery and cancellation as stream_live_data, for selected blocks.
    /// A failed block discards the selected sample and restarts its first block.
    pub fn stream_live_data_selected(
        &mut self,
        selection: LiveDataSelection,
        should_stop: &dyn Fn() -> bool,
        on_event: &mut dyn FnMut(SelectedLiveDataEvent) -> ControlFlow<()>,
    ) -> Result<(), Error> {
        selection.validate()?;
        match self.baud_rate() {
            Some(BaudRate::B9600) => {}
            Some(rate) => return Err(Error::LiveDataBaudRate(rate.as_u32())),
            None => return Err(Error::Unsynchronized),
        }
        let previous_gap = self.protocol.set_request_gap(Duration::from_millis(100));
        let result = self.run_live_stream(selection, should_stop, on_event);
        self.protocol.set_request_gap(previous_gap);
        result
    }

    fn run_live_stream(
        &mut self,
        selection: LiveDataSelection,
        should_stop: &dyn Fn() -> bool,
        on_event: &mut dyn FnMut(SelectedLiveDataEvent) -> ControlFlow<()>,
    ) -> Result<(), Error> {
        let mut failures = 0_u64;
        while !should_stop() {
            let event = match self.next_live_sample(selection, should_stop) {
                Ok(Some(data)) => {
                    failures = 0;
                    SelectedLiveDataEvent::Sample(Box::new(data))
                }
                Ok(None) => break,
                Err(_) if should_stop() => break,
                Err(error) if recoverable(&error) => {
                    failures = failures.saturating_add(1);
                    SelectedLiveDataEvent::Recovering {
                        consecutive_failures: failures,
                        error,
                    }
                }
                Err(error) => return Err(error),
            };
            if should_stop() || on_event(event).is_break() {
                break;
            }
            if failures != 0 {
                recovery_wait(failures, should_stop);
            }
        }
        Ok(())
    }

    fn next_live_sample(
        &mut self,
        selection: LiveDataSelection,
        should_stop: &dyn Fn() -> bool,
    ) -> Result<Option<SelectedLiveData>, Error> {
        if matches!(self.state, LinkState::Unidentified) {
            self.identify()?;
        }
        self.read_selected_sample(selection, should_stop)
    }
}

fn recoverable(error: &Error) -> bool {
    match error {
        Error::LiveDataRead { source, .. } => recoverable(source),
        Error::Timeout { .. }
        | Error::EchoMismatch
        | Error::InvalidLength(_)
        | Error::InvalidAddress(_)
        | Error::InvalidChecksum
        | Error::InvalidText { .. }
        | Error::Rejected(_) => true,
        // serialport may erase a POSIX unplug errno into Other during setup.
        // Only explicitly temporary setup errors can recover on the same handle.
        Error::TransportSetup { source, .. } => matches!(
            source.kind(),
            ErrorKind::TimedOut | ErrorKind::WouldBlock | ErrorKind::Interrupted
        ),
        Error::Io { source, .. } => !matches!(
            source.kind(),
            ErrorKind::NotConnected
                | ErrorKind::BrokenPipe
                | ErrorKind::ConnectionAborted
                | ErrorKind::ConnectionReset
                | ErrorKind::PermissionDenied
                | ErrorKind::NotFound
                | ErrorKind::UnexpectedEof
                | ErrorKind::Unsupported
                | ErrorKind::InvalidInput
        ),
        _ => false,
    }
}

fn recovery_wait(failures: u64, should_stop: &dyn Fn() -> bool) {
    // No permanent stop after packet failures, but no tight loop on a dead ECU.
    let millis = (100_u64 << (failures - 1).min(4)).min(1000);
    let mut remaining = Duration::from_millis(millis);
    while !remaining.is_zero() && !should_stop() {
        let slice = remaining.min(Duration::from_millis(20));
        std::thread::sleep(slice);
        remaining -= slice;
    }
}
