package com.nexus.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nexus.app.ui.theme.LocalNexusColors
import com.nexus.app.ui.theme.LocalNexusSpacing
import com.nexus.app.ui.theme.LocalNexusTypography
import com.nexus.app.ui.theme.PillShape

@Composable
fun PillTabs(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalNexusColors.current
    val type = LocalNexusTypography.current
    val spacing = LocalNexusSpacing.current

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        contentPadding = PaddingValues(horizontal = spacing.screen)
    ) {
        itemsIndexed(items) { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(if (selected) c.lime else c.surface)
                    .clickable { onSelect(index) }
                    .defaultMinSize(minHeight = spacing.touchTarget)
                    .padding(horizontal = spacing.md, vertical = spacing.xs),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = label,
                    style = type.body.copy(
                        color = if (selected) c.ink else c.textMuted,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                )
            }
        }
    }
}