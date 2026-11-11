package com.nexus.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.nexus.app.ui.theme.LocalNexusColors
import com.nexus.app.ui.theme.LocalNexusSpacing
import com.nexus.app.ui.theme.LocalNexusTypography

@Composable
fun NexusMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopEnd,
    offset: IntOffset = IntOffset(0, 48),
    content: @Composable ColumnScope.() -> Unit
) {
    if (!expanded) return
    val c = LocalNexusColors.current
    Popup(
        alignment = alignment,
        offset = offset,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            modifier = modifier
                .widthIn(min = 180.dp, max = 280.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(c.surface)
                .padding(vertical = 4.dp),
            content = content
        )
    }
}

@Composable
fun NexusMenuItem(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    selected: Boolean = false
) {
    val c = LocalNexusColors.current
    val type = LocalNexusTypography.current
    val spacing = LocalNexusSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = c.text,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(spacing.sm))
        }
        BasicText(
            text = text,
            style = type.body.copy(color = c.text),
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = c.limeDeep,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}