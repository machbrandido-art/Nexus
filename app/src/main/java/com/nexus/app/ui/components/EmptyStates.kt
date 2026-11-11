package com.nexus.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.nexus.app.ui.theme.LocalNexusColors
import com.nexus.app.ui.theme.LocalNexusSpacing
import com.nexus.app.ui.theme.LocalNexusTypography

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val c = LocalNexusColors.current
    val type = LocalNexusTypography.current
    val spacing = LocalNexusSpacing.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            drawGeometricShapes(c.lime, c.ink, c.surfaceAlt)
        }
        BasicText(
            text = title,
            style = type.title.copy(color = c.text),
            modifier = Modifier.padding(top = spacing.md)
        )
        BasicText(
            text = subtitle,
            style = type.body.copy(color = c.textMuted),
            modifier = Modifier.padding(top = spacing.xs, start = spacing.lg, end = spacing.lg)
        )
    }
}

private fun DrawScope.drawGeometricShapes(lime: Color, ink: Color, surfaceAlt: Color) {
    val w = size.width
    val h = size.height

    val halfCircle = Path().apply {
        moveTo(w * 0.10f, h * 0.85f)
        cubicTo(w * 0.20f, h * 0.55f, w * 0.80f, h * 0.55f, w * 0.90f, h * 0.85f)
        close()
    }
    drawPath(halfCircle, lime)

    drawCircle(
        color = ink,
        radius = w * 0.18f,
        center = Offset(w * 0.72f, h * 0.35f)
    )

    drawCircle(
        color = lime,
        radius = w * 0.07f,
        center = Offset(w * 0.28f, h * 0.28f)
    )

    drawRoundRect(
        color = surfaceAlt,
        topLeft = Offset(w * 0.15f, h * 0.60f),
        size = Size(w * 0.42f, h * 0.10f),
        cornerRadius = CornerRadius(h * 0.05f, h * 0.05f)
    )
}

@Composable
fun NexusProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color? = null,
    fillColor: Color? = null
) {
    val c = LocalNexusColors.current
    val track = trackColor ?: c.ink
    val fill = fillColor ?: c.lime
    val clamped = progress.coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
    ) {
        val radius = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(color = track, cornerRadius = radius)
        drawRoundRect(
            color = fill,
            size = Size(size.width * clamped, size.height),
            cornerRadius = radius
        )
    }
}