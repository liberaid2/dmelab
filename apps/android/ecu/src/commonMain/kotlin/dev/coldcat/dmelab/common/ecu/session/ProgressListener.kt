package dev.coldcat.dmelab.common.ecu.session

import dev.coldcat.dmelab.common.ecu.model.Progress

fun interface ProgressListener {
    fun onProgress(progress: Progress)
}
