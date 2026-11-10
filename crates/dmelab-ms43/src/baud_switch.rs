use std::time::Duration;

use crate::session::LinkState;
use crate::{BaudRate, Error, Session};

#[cfg(test)]
mod tests;

impl Session {
    /// Last known negotiated speed; `None` means a switch left the link uncertain.
    pub fn baud_rate(&self) -> Option<BaudRate> {
        match self.state {
            LinkState::Unidentified => Some(BaudRate::B9600),
            LinkState::Ready(rate) => Some(rate),
            LinkState::Unsynchronized => None,
        }
    }

    /// Negotiate a new speed, then change the local UART after a 200 ms delay.
    ///
    /// Requires successful identification. A lost/corrupt acknowledgement or a
    /// local configuration failure blocks further operations on this session.
    /// No automatic retry or rate guessing is performed.
    pub fn change_baud_rate(&mut self, target: BaudRate) -> Result<(), Error> {
        let previous = match self.state {
            LinkState::Unidentified => return Err(Error::NotIdentified),
            LinkState::Ready(rate) => rate,
            LinkState::Unsynchronized => return Err(Error::Unsynchronized),
        };
        if previous == target {
            return Ok(());
        }
        if target == BaudRate::B107000 {
            tracing::warn!(
                "107000 mode preserves MS4x Flasher's 100000 ECU / 107000 UART mapping; hardware validation is pending"
            );
        }
        self.protocol.prepare()?;
        // From the first transmitted byte, a failed exchange can leave the ECU
        // at a different rate. Only an explicit negative reply preserves it.
        self.state = LinkState::Unsynchronized;
        if let Err(error) = self.protocol.request(&target.payload()) {
            if matches!(error, Error::Rejected(0xB0)) {
                self.state = LinkState::Ready(previous);
            }
            return Err(error);
        }
        // MS4x Flasher UI field 0x04000148 = 200 ms, passed to communication
        // 0x06000008. The ECU acknowledgement is received at the old speed.
        std::thread::sleep(Duration::from_millis(200));
        self.protocol
            .set_baud_rate(target.as_u32())
            .map_err(|source| Error::BaudRateConfiguration {
                baud_rate: target.as_u32(),
                source,
            })?;
        self.state = LinkState::Ready(target);
        tracing::info!(baud_rate = target.as_u32(), "ECU and UART speed changed");
        Ok(())
    }

    /// Restore 9600 by negotiating at the current speed, then switching UART.
    /// This is explicit: dropping a session never sends hidden ECU commands.
    pub fn restore_default_baud_rate(&mut self) -> Result<(), Error> {
        self.change_baud_rate(BaudRate::B9600)?;
        self.protocol.prepare()
    }
}
