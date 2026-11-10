package dev.coldcat.dmelab.common.session.domain

/** Portable CSV basenames; the partial marker is reserved for interrupted recordings. */
object LiveDataLogName {
    fun stem(input: String, partial: Boolean): String? {
        var name = input.trim()
        if (name.endsWith(suffix = ".csv", ignoreCase = true)) name = name.dropLast(4)
        if (partial) name = name.removeSuffix("_partial")
        if (name.isBlank() || name.startsWith('.') || name.endsWith('.') || name.endsWith(' ')) return null
        if (!partial && name.endsWith("_partial")) return null
        if (name.any { it.code < 32 || it.code == 127 || it in "/\\:*?\"<>|" }) return null
        // Leave room for the collision counter, partial marker and extension within 255 bytes.
        if (name.encodeToByteArray().size > MAX_STEM_BYTES) return null
        return name
    }

    private const val MAX_STEM_BYTES = 200
}
