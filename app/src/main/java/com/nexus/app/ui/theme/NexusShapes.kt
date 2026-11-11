package com.nexus.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

val PillShape: Shape = RoundedCornerShape(percent = 50)
val CardShape: Shape = RoundedCornerShape(28.dp)
val Circle40: Shape = CircleShape

enum class NotchCorner { TopStart, TopEnd }

/**
 * Rounded rectangle with a circular notch cut from one top corner.
 * The badge slot in [com.nexus.app.ui.components.NotchedCard] is centred in
 * this notch so the badge reads as sitting *inside* the card with a 12dp gap
 * to the card body.
 *
 * - [notchInset]  distance from the top edge and the end edge to the notch centre.
 * - [notchRadius] radius of the cut. Badge half-height + 12dp is a good default.
 */
data class NotchedRoundedShape(
    val corner: Dp = 28.dp,
    val notchRadius: Dp = 22.dp,
    val notchInset: Dp = 28.dp,
    val notchCorner: NotchCorner = NotchCorner.TopEnd
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val c = with(density) { corner.toPx() }
        val r = with(density) { notchRadius.toPx() }
        val inset = with(density) { notchInset.toPx() }

        val base = Path().apply {
            addRoundRect(
                RoundRect(0f, 0f, size.width, size.height, CornerRadius(c, c))
            )
        }

        val cx = when (notchCorner) {
            NotchCorner.TopEnd ->
                if (layoutDirection == LayoutDirection.Rtl) inset else size.width - inset
            NotchCorner.TopStart ->
                if (layoutDirection == LayoutDirection.Rtl) size.width - inset else inset
        }
        val cy = inset

        val bite = Path().apply {
            addOval(Rect(cx - r, cy - r, cx + r, cy + r))
        }
        val cut = Path().apply { op(base, bite, PathOperation.Difference) }
        return Outline.Generic(cut)
    }
}