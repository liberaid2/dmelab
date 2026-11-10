package dev.coldcat.dmelab.common.ui.theme

import androidx.compose.ui.graphics.Color

/** Semantic dark palette. Feature UI consumes roles rather than individual hex values. */
internal object DmeColors {
    val background = Color(0xFF0B1018)
    val surfaceLow = Color(0xFF0E1621)
    val surface = Color(0xFF121A26)
    val surfaceRaised = Color(0xFF1A2636)
    val surfaceHigh = Color(0xFF223247)
    val outline = Color(0xFF29384B)
    val outlineStrong = Color(0xFF405775)

    val textPrimary = Color(0xFFF0F4FA)
    val textSecondary = Color(0xFFB1BED0)
    val textMuted = Color(0xFF8C9CB2)
    // Launcher violet and mint with accessible contrast; preserve the existing surfaces.
    // Keep accent aligned with dme_accent for platform controls outside Compose.
    val accent = Color(0xFFC46AF0)
    val onAccent = Color(0xFF291535)
    val accentContainer = Color(0xFF302039)
    val onAccentContainer = Color(0xFFF2DDFB)
    val accentOutline = Color(0xFF74488A)
    val mint = Color(0xFF94DEB4)
    val onMint = Color(0xFF10291F)
    val mintContainer = Color(0xFF1B3029)
    val onMintContainer = Color(0xFFD4EBDD)

    val info = DmeStatusColors(
        content = accent,
        container = Color(0xFF251A2C),
        outline = Color(0xFF61406D),
    )
    val success = DmeStatusColors(
        content = mint,
        container = Color(0xFF162C22),
        outline = Color(0xFF365844),
    )
    val warning = DmeStatusColors(
        content = Color(0xFFF2CB80),
        container = Color(0xFF302719),
        outline = Color(0xFF695232),
    )
    val error = DmeStatusColors(
        content = Color(0xFFFFADB5),
        container = Color(0xFF341E27),
        outline = Color(0xFF70404D),
    )
}
