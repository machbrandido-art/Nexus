package com.nexus.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nexus.app.ui.theme.LocalNexusColors
import com.nexus.app.ui.theme.NotchCorner
import com.nexus.app.ui.theme.NotchedRoundedShape

@Composable
fun NotchedCard(
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    badgeVariant: BadgeVariant = BadgeVariant.Ink,
    notchCorner: NotchCorner = NotchCorner.TopEnd,
    background: Color = LocalNexusColors.current.surface,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val notchRadius = 24.dp
    val notchInset = 28.dp
    val shape = if (badgeText != null) {
        NotchedRoundedShape(
            corner = 28.dp,
            notchRadius = notchRadius,
            notchInset = notchInset,
            notchCorner = notchCorner
        )
    } else {
        RoundedCornerShape(28.dp)
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(background, shape = shape)
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = content
            )
        }

        if (badgeText != null) {
            val alignment = when (notchCorner) {
                NotchCorner.TopStart -> Alignment.TopStart
                NotchCorner.TopEnd -> Alignment.TopEnd
            }
            val insetPadding = notchInset - notchRadius
            val padModifier = when (notchCorner) {
                NotchCorner.TopStart ->
                    Modifier.padding(top = insetPadding, start = insetPadding)
                NotchCorner.TopEnd ->
                    Modifier.padding(top = insetPadding, end = insetPadding)
            }
            Box(
                modifier = Modifier
                    .align(alignment)
                    .then(padModifier)
                    .size(notchRadius * 2),
                contentAlignment = Alignment.Center
            ) {
                Badge(text = badgeText, variant = badgeVariant)
            }
        }
    }
}