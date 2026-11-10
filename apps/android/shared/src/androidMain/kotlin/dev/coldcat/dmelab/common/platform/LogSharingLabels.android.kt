package dev.coldcat.dmelab.common.platform

import dev.coldcat.dmelab.shared.resources.Res
import dev.coldcat.dmelab.shared.resources.live_log_archive_share
import dev.coldcat.dmelab.shared.resources.live_log_selection_active
import dev.coldcat.dmelab.shared.resources.live_log_selection_hint
import dev.coldcat.dmelab.shared.resources.live_log_share

internal actual fun logSharingLabels(): LogSharingLabels = LogSharingLabels(
    action = Res.string.live_log_share,
    archiveAction = Res.string.live_log_archive_share,
    selectionHint = Res.string.live_log_selection_hint,
    selectionActive = Res.string.live_log_selection_active,
)
