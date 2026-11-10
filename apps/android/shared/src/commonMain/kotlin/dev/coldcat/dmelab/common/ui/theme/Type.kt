package dev.coldcat.dmelab.common.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

internal object DmeTypography {
    val material = Typography(
        headlineLarge = textStyle(size = 32, lineHeight = 40, weight = FontWeight.Bold),
        headlineMedium = textStyle(size = 28, lineHeight = 36, weight = FontWeight.SemiBold),
        headlineSmall = textStyle(size = 24, lineHeight = 32, weight = FontWeight.SemiBold),
        titleLarge = textStyle(size = 22, lineHeight = 28, weight = FontWeight.SemiBold),
        titleMedium = textStyle(size = 18, lineHeight = 24, weight = FontWeight.SemiBold),
        titleSmall = textStyle(size = 16, lineHeight = 24, weight = FontWeight.SemiBold),
        bodyLarge = textStyle(size = 16, lineHeight = 24, weight = FontWeight.Normal),
        bodyMedium = textStyle(size = 14, lineHeight = 22, weight = FontWeight.Normal),
        bodySmall = textStyle(size = 12, lineHeight = 18, weight = FontWeight.Normal),
        labelLarge = textStyle(size = 14, lineHeight = 20, weight = FontWeight.SemiBold),
        labelMedium = textStyle(size = 12, lineHeight = 16, weight = FontWeight.Medium),
        labelSmall = textStyle(size = 11, lineHeight = 16, weight = FontWeight.Medium),
    )

    val brand = textStyle(size = 18, lineHeight = 24, weight = FontWeight.ExtraBold)
        .copy(letterSpacing = 1.sp)
    val identifier = textStyle(size = 16, lineHeight = 24, weight = FontWeight.Medium)
        .copy(fontFamily = FontFamily.Monospace)

    private fun textStyle(size: Int, lineHeight: Int, weight: FontWeight) = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = 0.sp,
    )
}
