package dev.coldcat.dmelab.common.ecu.session

import dev.coldcat.dmelab.common.ecu.model.LiveDataSelection
import dev.coldcat.dmelab.ffi.LiveDataSelection as NativeLiveDataSelection

internal fun LiveDataSelection.toNative(): NativeLiveDataSelection = NativeLiveDataSelection(
    measurements = measurements, vanos = vanos, digital = digital,
)
