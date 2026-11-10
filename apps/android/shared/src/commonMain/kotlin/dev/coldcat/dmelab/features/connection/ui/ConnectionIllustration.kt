package dev.coldcat.dmelab.features.connection.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.coldcat.dmelab.common.ui.theme.DMELabTheme
import dev.coldcat.dmelab.common.ui.theme.DmeColors
import dev.coldcat.dmelab.features.connection.presentation.ConnectionScreenState.Step

/** Decorative hardware diagram; the adjacent text provides its accessible explanation. */
@Composable
internal fun ConnectionIllustration(step: Step, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.primary
    val soft = DmeColors.accentContainer
    val detail = MaterialTheme.colorScheme.secondary
    val fill = MaterialTheme.colorScheme.surface
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(IllustrationHeight)
            .background(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.large),
    ) {
        val unit = size.height / 176f
        val center = size.width / 2f
        fun point(x: Float, y: Float) = Offset(center + x * unit, y * unit)
        val stroke = Stroke(width = 2.5f * unit)
        drawCircle(color = soft, radius = 70f * unit, center = point(0f, 88f))
        when (step) {
            Step.Cable -> {
                drawRoundRect(color = fill, topLeft = point(-89f, 27f), size = Size(64f * unit, 118f * unit), cornerRadius = CornerRadius(11f * unit))
                drawRoundRect(color = ink, topLeft = point(-89f, 27f), size = Size(64f * unit, 118f * unit), cornerRadius = CornerRadius(11f * unit), style = stroke)
                drawLine(color = ink, start = point(-68f, 35f), end = point(-46f, 35f), strokeWidth = 3f * unit)
                drawLine(color = ink, start = point(-63f, 136f), end = point(-51f, 136f), strokeWidth = 3f * unit)
                val cable = Path().apply {
                    moveTo(center - 57f * unit, 146f * unit)
                    cubicTo(center - 57f * unit, 176f * unit, center + 61f * unit, 163f * unit, center + 61f * unit, 124f * unit)
                }
                drawPath(path = cable, color = detail, style = Stroke(width = 3f * unit))
                drawRoundRect(color = fill, topLeft = point(25f, 65f), size = Size(73f * unit, 59f * unit), cornerRadius = CornerRadius(8f * unit))
                drawRoundRect(color = ink, topLeft = point(25f, 65f), size = Size(73f * unit, 59f * unit), cornerRadius = CornerRadius(8f * unit), style = stroke)
                drawRoundRect(color = ink, topLeft = point(35f, 47f), size = Size(53f * unit, 20f * unit), cornerRadius = CornerRadius(4f * unit), style = stroke)
                repeat(4) { index ->
                    drawCircle(color = ink, radius = 2.3f * unit, center = point(46f + index * 10f, 57f))
                }
                drawLine(color = ink, start = point(45f, 89f), end = point(78f, 89f), strokeWidth = 3f * unit)
                drawLine(color = ink, start = point(51f, 101f), end = point(72f, 101f), strokeWidth = 2f * unit)
            }
            Step.Ecu -> {
                drawRoundRect(color = fill, topLeft = point(-82f, 37f), size = Size(164f * unit, 102f * unit), cornerRadius = CornerRadius(12f * unit))
                drawRoundRect(color = ink, topLeft = point(-82f, 37f), size = Size(164f * unit, 102f * unit), cornerRadius = CornerRadius(12f * unit), style = stroke)
                repeat(5) { index ->
                    drawLine(color = detail, start = point(-66f, 55f + index * 16f), end = point(-39f, 55f + index * 16f), strokeWidth = 3f * unit)
                }
                drawRoundRect(color = soft, topLeft = point(-23f, 59f), size = Size(57f * unit, 57f * unit), cornerRadius = CornerRadius(8f * unit))
                drawRoundRect(color = ink, topLeft = point(-14f, 68f), size = Size(39f * unit, 39f * unit), cornerRadius = CornerRadius(4f * unit), style = stroke)
                repeat(4) { index ->
                    drawLine(color = ink, start = point(-6f + index * 8f, 61f), end = point(-6f + index * 8f, 68f), strokeWidth = 2f * unit)
                    drawLine(color = ink, start = point(-6f + index * 8f, 107f), end = point(-6f + index * 8f, 114f), strokeWidth = 2f * unit)
                }
                drawRoundRect(color = ink, topLeft = point(70f, 63f), size = Size(26f * unit, 48f * unit), cornerRadius = CornerRadius(4f * unit), style = stroke)
                repeat(4) { index ->
                    drawCircle(color = ink, radius = 2f * unit, center = point(83f, 73f + index * 10f))
                }
            }
        }
    }
}

// Local artwork geometry is not a shared layout token.
private val IllustrationHeight = 176.dp

@Preview(name = "Кабель · фиолетовый и мятный", showBackground = true, widthDp = 360)
@Composable
private fun CableIllustrationPreview() {
    DMELabTheme { ConnectionIllustration(step = Step.Cable) }
}

@Preview(name = "ЭБУ · фиолетовый и мятный", showBackground = true, widthDp = 360)
@Composable
private fun EcuIllustrationPreview() {
    DMELabTheme { ConnectionIllustration(step = Step.Ecu) }
}
