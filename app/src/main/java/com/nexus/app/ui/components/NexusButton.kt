package com.nexus.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nexus.app.ui.theme.LocalNexusColors
import com.nexus.app.ui.theme.LocalNexusSpacing
import com.nexus.app.ui.theme.LocalNexusTypography
import com.nexus.app.ui.theme.PillShape

enum class NexusButtonVariant { Ink, Lime, Surface, Outline }

@Composable
fun NexusButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: NexusButtonVariant = NexusButtonVariant.Ink,
    enabled: Boolean = true,
    fullWidth: Boolean = false
) {
    val colors = LocalNexusColors.current
    val type = LocalNexusTypography.current
    val spacing = LocalNexusSpacing.current
    val (bg, fg) = variantColors(variant)

    Box(
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .clip(PillShape)
            .then(
                if (variant == NexusButtonVariant.Outline)
                    Modifier.border(1.dp, fg, PillShape)
                else Modifier
            )
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .defaultMinSize(minHeight = spacing.touchTarget)
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = type.subtitle.copy(
                color = if (enabled) fg else fg.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: NexusButtonVariant = NexusButtonVariant.Ink,
    size: Dp = 40.dp,
    enabled: Boolean = true
) {
    val (bg, fg) = variantColors(variant)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) fg else fg.copy(alpha = 0.5f),
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

@Composable
private fun variantColors(variant: NexusButtonVariant): Pair<Color, Color> {
    val c = LocalNexusColors.current
    return when (variant) {
        NexusButtonVariant.Ink -> c.ink to c.onInk
        NexusButtonVariant.Lime -> c.lime to c.ink
        NexusButtonVariant.Surface -> c.surface to c.text
        NexusButtonVariant.Outline -> Color.Transparent to c.text
    }
}