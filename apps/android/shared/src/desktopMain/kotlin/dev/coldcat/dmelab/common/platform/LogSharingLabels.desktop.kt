package dev.coldcat.dmelab.common.platform

import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.mac_archive_reveal
import dev.coldcat.dmelab.shared.resources.mac_log_reveal
import dev.coldcat.dmelab.shared.resources.mac_log_selection_active
import dev.coldcat.dmelab.shared.resources.mac_log_selection_hint

internal actual fun logSharingLabels(): LogSharingLabels = LogSharingLabels(
    action = Res.string.mac_log_reveal,
    archiveAction = Res.string.mac_archive_reveal,
    selectionHint = Res.string.mac_log_selection_hint,
    selectionActive = Res.string.mac_log_selection_active,
)
