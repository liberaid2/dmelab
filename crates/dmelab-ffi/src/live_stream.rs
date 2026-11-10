//! A blocking streaming call keeps Session local, without Send bounds on Transport.

use std::ops::ControlFlow;
use std::sync::Arc;
use std::sync::atomic::{AtomicBool, Ordering};

use dmelab_ms43::{LiveDataEvent, SelectedLiveDataEvent};

use crate::{
    DmeError, LiveData, LiveDataSelection, Ms43Client, SelectedLiveData, TransportFailure,
};

/// One monotonic stop flag per stream. Stopping never needs the busy client lock.
#[derive(uniffi::Object)]
pub struct LiveDataStreamControl {
    stopped: AtomicBool,
}

#[uniffi::export]
impl LiveDataStreamControl {
    #[uniffi::constructor]
    pub fn new() -> Arc<Self> {
        Arc::new(Self {
            stopped: AtomicBool::new(false),
        })
    }

    /// May be called from another application thread or from a listener.
    pub fn stop(&self) {
        self.stopped.store(true, Ordering::Relaxed);
    }
}

impl LiveDataStreamControl {
    fn is_stopped(&self) -> bool {
        self.stopped.load(Ordering::Relaxed)
    }
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct LiveDataStreamFailure {
    /// None means identification failed before the selected data blocks.
    pub command: Option<u8>,
    pub block: Option<u8>,
    pub consecutive_failures: u64,
    pub details: String,
}

#[uniffi::export(with_foreign)]
pub trait LiveDataListener: Send + Sync {
    fn on_sample(&self, data: LiveData) -> Result<(), TransportFailure>;
    /// Recoverable error: polling continues; previous values are not a new sample.
    fn on_recovering(&self, failure: LiveDataStreamFailure) -> Result<(), TransportFailure>;
}

#[uniffi::export(with_foreign)]
pub trait SelectedLiveDataListener: Send + Sync {
    fn on_sample(&self, data: SelectedLiveData) -> Result<(), TransportFailure>;
    fn on_recovering(&self, failure: LiveDataStreamFailure) -> Result<(), TransportFailure>;
}

impl LiveDataStreamFailure {
    fn new(consecutive_failures: u64, error: dmelab_ms43::Error) -> Self {
        Self {
            command: match &error {
                dmelab_ms43::Error::LiveDataRead { command, .. } => Some(*command),
                _ => None,
            },
            block: match &error {
                dmelab_ms43::Error::LiveDataRead { block, .. } => Some(*block),
                _ => None,
            },
            consecutive_failures,
            details: error.to_string(),
        }
    }
}

#[uniffi::export]
impl Ms43Client {
    /// Configure/identify once, then poll at 9600 until stop or a terminal error.
    /// All callbacks execute synchronously on the calling thread.
    pub fn stream_live_data(
        &self,
        timeout_ms: u32,
        control: Arc<LiveDataStreamControl>,
        listener: Arc<dyn LiveDataListener>,
    ) -> Result<(), DmeError> {
        if control.is_stopped() {
            return Ok(());
        }
        let mut listener_error = None;
        self.execute(timeout_ms, |session| {
            session.stream_live_data(&|| control.is_stopped(), &mut |event| {
                let result = match event {
                    LiveDataEvent::Sample(data) => listener.on_sample((*data).into()),
                    LiveDataEvent::Recovering {
                        consecutive_failures,
                        error,
                    } => listener
                        .on_recovering(LiveDataStreamFailure::new(consecutive_failures, error)),
                };
                match result {
                    Ok(()) => ControlFlow::Continue(()),
                    Err(error) => {
                        listener_error = Some(error);
                        ControlFlow::Break(())
                    }
                }
            })
        })?;
        finish_listener(listener_error)
    }

    /// Poll exactly the selected blocks at 9600, with the same stop/retry contract.
    pub fn stream_live_data_selected(
        &self,
        selection: LiveDataSelection,
        timeout_ms: u32,
        control: Arc<LiveDataStreamControl>,
        listener: Arc<dyn SelectedLiveDataListener>,
    ) -> Result<(), DmeError> {
        let selection = dmelab_ms43::LiveDataSelection::from(selection);
        selection.validate()?;
        if control.is_stopped() {
            return Ok(());
        }
        let mut listener_error = None;
        self.execute(timeout_ms, |session| {
            session.stream_live_data_selected(selection, &|| control.is_stopped(), &mut |event| {
                let result = match event {
                    SelectedLiveDataEvent::Sample(data) => listener.on_sample((*data).into()),
                    SelectedLiveDataEvent::Recovering {
                        consecutive_failures,
                        error,
                    } => listener
                        .on_recovering(LiveDataStreamFailure::new(consecutive_failures, error)),
                };
                match result {
                    Ok(()) => ControlFlow::Continue(()),
                    Err(error) => {
                        listener_error = Some(error);
                        ControlFlow::Break(())
                    }
                }
            })
        })?;
        finish_listener(listener_error)
    }
}

fn finish_listener(error: Option<TransportFailure>) -> Result<(), DmeError> {
    match error {
        Some(error) => Err(DmeError::Transport {
            details: format!("live data listener failed: {error}"),
        }),
        None => Ok(()),
    }
}
