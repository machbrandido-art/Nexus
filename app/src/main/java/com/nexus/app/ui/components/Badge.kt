package com.nexus.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nexus.app.ui.theme.LocalNexusColors
import com.nexus.app.ui.theme.LocalNexusTypography
import com.nexus.app.ui.theme.PillShape

enum class BadgeVariant { Ink, Lime, Surface, Outline }

@Composable
fun Badge(
    text: String,
    modifier: Modifier = Modifier,
    variant: BadgeVariant = BadgeVariant.Ink
) {
    val c = LocalNexusColors.current
    val type = LocalNexusTypography.current
    val (bg, fg, border) = when (variant) {
        BadgeVariant.Ink -> Triple(c.ink, c.onInk, null)
        BadgeVariant.Lime -> Triple(c.lime, c.ink, null)
        BadgeVariant.Surface -> Triple(c.surface, c.text, null)
        BadgeVariant.Outline -> Triple(Color.Transparent, c.text, c.text)
    }

    Box(
        modifier = modifier
            .height(24.dp)
            .clip(PillShape)
            .then(if (border != null) Modifier.border(1.dp, border, PillShape) else Modifier)
            .background(bg)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = type.caption.copy(color = fg)
        )
    }
}