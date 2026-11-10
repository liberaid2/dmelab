package dev.coldcat.dmelab.common.session.data

import java.io.File
import org.koin.core.annotation.Single

@Single
internal class DesktopStorage {
    val directory = File(System.getProperty("user.home"), "Library/Application Support/DME Lab")
    val cacheDirectory = File(directory, "cache")

    fun prepare() {
        if (!directory.isDirectory && !directory.mkdirs()) throw java.io.IOException("Cannot create application storage")
    }
}
