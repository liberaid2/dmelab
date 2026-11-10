use std::sync::{Arc, Mutex, TryLockError};
use std::time::Duration;

use crate::transport::CallbackTransport;
use crate::{
    BaudRate, DmeError, Dtc, FlashMode, Identification, KlineTransport, LiveData, Progress,
    TransportFailure,
};

#[uniffi::export(with_foreign)]
pub trait ProgressListener: Send + Sync {
    /// Exceptions are logged and ignored so a UI failure cannot interrupt flash.
    fn on_progress(&self, progress: Progress) -> Result<(), TransportFailure>;
}

enum ClientState {
    Ready,
    NeedsReconnect,
}

enum ClientTransport {
    Callback(Arc<dyn KlineTransport>),
    #[cfg(feature = "desktop")]
    Serial(crate::serial::SharedSerialTransport),
}

/// One client per physical connection. All methods block the calling thread.
/// Concurrent/reentrant operations return Busy instead of waiting or deadlocking.
#[derive(uniffi::Object)]
pub struct Ms43Client {
    transport: ClientTransport,
    state: Mutex<ClientState>,
}

#[uniffi::export]
impl Ms43Client {
    #[uniffi::constructor]
    pub fn new(transport: Arc<dyn KlineTransport>) -> Arc<Self> {
        Arc::new(Self {
            transport: ClientTransport::Callback(transport),
            state: Mutex::new(ClientState::Ready),
        })
    }

    pub fn identify(&self, timeout_ms: u32) -> Result<Identification, DmeError> {
        self.execute(timeout_ms, |session| session.identify().map(Into::into))
    }

    /// Read one complete three-block set of measurements at 9600 baud.
    pub fn read_live_data(&self, timeout_ms: u32) -> Result<LiveData, DmeError> {
        self.execute(timeout_ms, |session| {
            session.read_live_data().map(Into::into)
        })
    }

    /// Return the complete stored fault list at 9600 baud, or an error.
    pub fn read_dtcs(&self, timeout_ms: u32) -> Result<Vec<Dtc>, DmeError> {
        self.execute(timeout_ms, |session| {
            session
                .read_dtcs()
                .map(|dtcs| dtcs.into_iter().map(Into::into).collect())
        })
    }

    /// Clear DTC memory once at 9600 baud; return only after an A0 acknowledgement.
    /// No retries or automatic readback. Recurring faults may reappear.
    pub fn clear_dtcs(&self, timeout_ms: u32) -> Result<(), DmeError> {
        self.execute(timeout_ms, |session| session.clear_dtcs())
    }

    pub fn read_flash(
        &self,
        mode: FlashMode,
        baud_rate: BaudRate,
        timeout_ms: u32,
        listener: Option<Arc<dyn ProgressListener>>,
    ) -> Result<Vec<u8>, DmeError> {
        self.execute(timeout_ms, |session| {
            session.read_flash(mode.into(), baud_rate.into(), &mut |progress| {
                report_progress(&listener, progress);
            })
        })
    }

    pub fn write_flash(
        &self,
        mode: FlashMode,
        image: Vec<u8>,
        baud_rate: BaudRate,
        timeout_ms: u32,
        listener: Option<Arc<dyn ProgressListener>>,
    ) -> Result<(), DmeError> {
        // Reject invalid files before configuring or touching the adapter.
        dmelab_ms43::validate_image(&image, mode.into())?;
        self.execute(timeout_ms, |session| {
            session.write_flash(mode.into(), &image, baud_rate.into(), &mut |progress| {
                report_progress(&listener, progress);
            })
        })
    }
}

#[cfg(feature = "desktop")]
#[uniffi::export]
impl Ms43Client {
    /// Own the native serial handle until the last reference to this client drops.
    /// Opening configures 9600 8E1; it does not send diagnostic commands.
    #[uniffi::constructor]
    pub fn open_serial(port_name: String, timeout_ms: u32) -> Result<Arc<Self>, DmeError> {
        let timeout = operation_timeout(timeout_ms)?;
        if port_name.trim().is_empty() {
            return Err(DmeError::InvalidArgument {
                details: "serial port name must not be empty".into(),
            });
        }
        let transport = crate::serial::SharedSerialTransport::open(&port_name, timeout)?;
        Ok(Arc::new(Self {
            transport: ClientTransport::Serial(transport),
            state: Mutex::new(ClientState::Ready),
        }))
    }
}

impl Ms43Client {
    pub(super) fn execute<T>(
        &self,
        timeout_ms: u32,
        operation: impl FnOnce(&mut dmelab_ms43::Session) -> Result<T, dmelab_ms43::Error>,
    ) -> Result<T, DmeError> {
        let timeout = operation_timeout(timeout_ms)?;
        let mut state = match self.state.try_lock() {
            Ok(state) => state,
            Err(TryLockError::WouldBlock) => return Err(DmeError::Busy),
            Err(TryLockError::Poisoned(_)) => return Err(DmeError::NeedsReconnect),
        };
        if matches!(*state, ClientState::NeedsReconnect) {
            return Err(DmeError::NeedsReconnect);
        }
        let mut session = match &self.transport {
            ClientTransport::Callback(callback) => {
                callback
                    .configure_initial()
                    .map_err(|error| DmeError::Transport {
                        details: error.to_string(),
                    })?;
                let transport = CallbackTransport::new(Arc::clone(callback), timeout_ms);
                dmelab_ms43::Session::new(transport, timeout)?
            }
            #[cfg(feature = "desktop")]
            ClientTransport::Serial(transport) => {
                transport
                    .configure_initial()
                    .map_err(|error| DmeError::Transport {
                        details: error.to_string(),
                    })?;
                dmelab_ms43::Session::new(transport.clone(), timeout)?
            }
        };
        let result = operation(&mut session);
        if session.baud_rate() != Some(dmelab_ms43::BaudRate::B9600) {
            *state = ClientState::NeedsReconnect;
        }
        result.map_err(Into::into)
    }
}

fn operation_timeout(timeout_ms: u32) -> Result<Duration, DmeError> {
    if !(1..=60_000).contains(&timeout_ms) {
        return Err(DmeError::InvalidArgument {
            details: "timeout must be 1..60000 ms".into(),
        });
    }
    Ok(Duration::from_millis(timeout_ms.into()))
}

fn report_progress(
    listener: &Option<Arc<dyn ProgressListener>>,
    progress: dmelab_ms43::FlashProgress,
) {
    if let Some(listener) = listener
        && let Err(error) = listener.on_progress(progress.into())
    {
        tracing::warn!(%error, "progress listener failed; continuing flash operation");
    }
}
