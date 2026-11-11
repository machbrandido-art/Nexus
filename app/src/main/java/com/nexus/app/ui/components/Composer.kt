package com.nexus.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nexus.app.R
import com.nexus.app.ui.theme.LocalNexusColors
import com.nexus.app.ui.theme.LocalNexusSpacing
import com.nexus.app.ui.theme.LocalNexusTypography

@Composable
fun Composer(
    input: String,
    isStreaming: Boolean,
    currentModel: String?,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onOpenModelPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalNexusColors.current
    val type = LocalNexusTypography.current
    val spacing = LocalNexusSpacing.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm)
    ) {
        if (currentModel != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(c.surfaceAlt)
                    .clickable(onClick = onOpenModelPicker)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                BasicText(
                    text = currentModel,
                    style = type.caption.copy(color = c.textMuted)
                )
            }
            Spacer(Modifier.size(spacing.xs))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(c.ink)
                .padding(start = spacing.md, end = spacing.xs, top = spacing.xs, bottom = spacing.xs),
            verticalAlignment = Alignment.Bottom
        ) {
            BasicTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp),
                textStyle = type.body.copy(color = c.onInk),
                cursorBrush = SolidColor(c.lime),
                maxLines = 6,
                decorationBox = { inner ->
                    Box {
                        if (input.isEmpty()) {
                            BasicText(
                                text = stringResource(R.string.chat_input_hint),
                                style = type.body.copy(color = c.onInk.copy(alpha = 0.5f))
                            )
                        }
                        inner()
                    }
                }
            )
            Spacer(Modifier.width(spacing.xs))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(c.lime)
                    .clickable { if (isStreaming) onStop() else onSend() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isStreaming) Icons.Filled.Stop
                    else Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(
                        if (isStreaming) R.string.action_stop else R.string.action_send
                    ),
                    tint = c.ink,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}