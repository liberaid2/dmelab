package dev.coldcat.dmelab.common.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DmeColors.accent,
    onPrimary = DmeColors.onAccent,
    primaryContainer = DmeColors.accentContainer,
    onPrimaryContainer = DmeColors.onAccentContainer,
    secondary = DmeColors.mint,
    onSecondary = DmeColors.onMint,
    secondaryContainer = DmeColors.mintContainer,
    onSecondaryContainer = DmeColors.onMintContainer,
    tertiary = DmeColors.textSecondary,
    onTertiary = DmeColors.background,
    tertiaryContainer = DmeColors.surfaceHigh,
    onTertiaryContainer = DmeColors.textPrimary,
    error = DmeColors.error.content,
    onError = DmeColors.background,
    errorContainer = DmeColors.error.container,
    onErrorContainer = DmeColors.error.content,
    background = DmeColors.background,
    onBackground = DmeColors.textPrimary,
    surface = DmeColors.surface,
    onSurface = DmeColors.textPrimary,
    surfaceVariant = DmeColors.surfaceRaised,
    onSurfaceVariant = DmeColors.textSecondary,
    surfaceDim = DmeColors.surfaceLow,
    surfaceBright = DmeColors.surfaceHigh,
    surfaceContainerLowest = DmeColors.background,
    surfaceContainerLow = DmeColors.surfaceLow,
    surfaceContainer = DmeColors.surface,
    surfaceContainerHigh = DmeColors.surfaceRaised,
    surfaceContainerHighest = DmeColors.surfaceHigh,
    outline = DmeColors.outlineStrong,
    outlineVariant = DmeColors.outline,
    inverseSurface = DmeColors.textPrimary,
    inverseOnSurface = DmeColors.background,
    inversePrimary = DmeColors.onAccent,
    surfaceTint = DmeColors.accent,
)

/** Fixed dark theme. System appearance and wallpaper do not change semantic colors. */
@Composable
fun DMELabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = DmeTypography.material,
        shapes = DmeShapes,
        content = content,
    )
}
