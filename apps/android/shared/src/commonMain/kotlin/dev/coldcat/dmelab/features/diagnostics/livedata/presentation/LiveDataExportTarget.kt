package dev.coldcat.dmelab.features.diagnostics.livedata.presentation

internal sealed interface LiveDataExportTarget {
    val id: String
    val format: Format

    data class Csv(override val id: String) : LiveDataExportTarget {
        override val format = Format.Csv
    }

    data class Archive(override val id: String) : LiveDataExportTarget {
        override val format = Format.Zip
    }

    enum class Format(val mimeType: String) {
        Csv("text/csv"),
        Zip("application/zip"),
    }
}
