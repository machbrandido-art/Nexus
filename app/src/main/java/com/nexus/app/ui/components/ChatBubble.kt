package com.nexus.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.nexus.app.providers.ChatMessage
import com.nexus.app.ui.theme.LocalNexusColors
import com.nexus.app.ui.theme.LocalNexusSpacing
import com.nexus.app.ui.theme.LocalNexusTypography

@Composable
fun ChatBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    reasoning: String? = null
) {
    val c = LocalNexusColors.current
    val spacing = LocalNexusSpacing.current
    val isUser = message.role == ChatMessage.Role.USER
    val bg = if (isUser) c.ink else c.surface
    val fg = if (isUser) c.onInk else c.text

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        if (!isUser && !reasoning.isNullOrBlank()) {
            ReasoningRow(text = reasoning)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (isUser) 0.88f else 1f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(bg)
                    .padding(horizontal = spacing.md, vertical = spacing.sm)
            ) {
                MarkdownBody(text = message.content, color = fg)
            }
        }
    }
}

@Composable
private fun MarkdownBody(text: String, color: Color) {
    val type = LocalNexusTypography.current
    val parts = remember(text) { splitMarkdown(text) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        parts.forEach { part ->
            when (part) {
                is MdPart.Prose -> {
                    if (part.text.isNotBlank()) {
                        BasicText(
                            text = part.text.trim(),
                            style = type.body.copy(color = color)
                        )
                    }
                }
                is MdPart.Code -> CodeBlock(language = part.lang, code = part.code)
            }
        }
    }
}

@Composable
fun CodeBlock(
    language: String?,
    code: String,
    modifier: Modifier = Modifier
) {
    val c = LocalNexusColors.current
    val type = LocalNexusTypography.current
    val spacing = LocalNexusSpacing.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.ink)
            .padding(spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = (language ?: "code").lowercase(),
                style = type.caption.copy(color = c.lime)
            )
            Spacer(Modifier.weight(1f))
            CircleIconButton(
                icon = Icons.Filled.ContentCopy,
                contentDescription = if (copied) "Copied" else "Copy code",
                onClick = {
                    clipboard.setText(AnnotatedString(code))
                    copied = true
                },
                variant = NexusButtonVariant.Surface,
                size = 32.dp
            )
        }
        Spacer(Modifier.size(6.dp))
        BasicText(
            text = highlighted(code),
            style = type.mono.copy(color = c.onInk)
        )
    }
}

@Composable
fun ReasoningRow(
    text: String,
    modifier: Modifier = Modifier
) {
    val c = LocalNexusColors.current
    val type = LocalNexusTypography.current
    val spacing = LocalNexusSpacing.current
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.surfaceAlt)
            .clickable { expanded = !expanded }
            .padding(spacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                text = "Reasoning",
                style = type.caption.copy(color = c.textMuted, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = c.textMuted,
                modifier = Modifier.size(16.dp)
            )
        }
        if (expanded) {
            Spacer(Modifier.size(6.dp))
            BasicText(
                text = text,
                style = type.caption.copy(color = c.textMuted)
            )
        }
    }
}

// ---- Markdown splitter ----

private sealed class MdPart {
    data class Prose(val text: String) : MdPart()
    data class Code(val lang: String?, val code: String) : MdPart()
}

private fun splitMarkdown(text: String): List<MdPart> {
    if (!text.contains("```")) return listOf(MdPart.Prose(text))
    val out = mutableListOf<MdPart>()
    var cursor = 0
    while (cursor < text.length) {
        val open = text.indexOf("```", cursor)
        if (open < 0) {
            out += MdPart.Prose(text.substring(cursor))
            break
        }
        if (open > cursor) out += MdPart.Prose(text.substring(cursor, open))
        val afterOpen = open + 3
        val newline = text.indexOf('\n', afterOpen)
        if (newline < 0) { out += MdPart.Prose(text.substring(open)); break }
        val lang = text.substring(afterOpen, newline).trim().ifBlank { null }
        val close = text.indexOf("```", newline + 1)
        if (close < 0) { out += MdPart.Prose(text.substring(open)); break }
        out += MdPart.Code(lang, text.substring(newline + 1, close))
        cursor = close + 3
        if (cursor < text.length && text[cursor] == '\n') cursor++
    }
    return out
}

// ---- Code highlighter ----

private val KEYWORDS = setOf(
    "fun", "val", "var", "class", "object", "interface", "enum", "data", "sealed",
    "abstract", "open", "override", "private", "public", "protected", "internal",
    "return", "if", "else", "when", "for", "while", "do", "break", "continue",
    "try", "catch", "finally", "throw", "in", "is", "as", "null", "true", "false",
    "this", "super", "import", "package", "companion", "init", "constructor",
    "by", "lazy", "suspend", "inline", "noinline", "crossinline",
    "def", "None", "True", "False", "elif", "import", "from", "with", "lambda",
    "function", "const", "let", "new", "typeof", "async", "await", "export"
)

@Composable
private fun highlighted(code: String): AnnotatedString {
    val c = LocalNexusColors.current
    val keywordColor = c.lime
    val stringColor = c.limeSoft
    val commentColor = c.textMuted
    val defaultColor = c.onInk

    return buildAnnotatedString {
        var i = 0
        while (i < code.length) {
            val ch = code[i]
            if (ch == '/' && i + 1 < code.length && code[i + 1] == '/') {
                val end = code.indexOf('\n', i).let { if (it < 0) code.length else it }
                withStyle(SpanStyle(color = commentColor)) {
                    append(code.substring(i, end))
                }
                i = end
                continue
            }
            if (ch == '"' || ch == '\'') {
                val quote = ch
                var j = i + 1
                while (j < code.length && code[j] != quote && code[j] != '\n') {
                    if (code[j] == '\\' && j + 1 < code.length) j++
                    j++
                }
                if (j < code.length && code[j] == quote) j++
                withStyle(SpanStyle(color = stringColor)) {
                    append(code.substring(i, j))
                }
                i = j
                continue
            }
            if (ch.isLetter() || ch == '_') {
                var j = i
                while (j < code.length && (code[j].isLetterOrDigit() || code[j] == '_')) j++
                val word = code.substring(i, j)
                if (word in KEYWORDS) {
                    withStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.SemiBold)) {
                        append(word)
                    }
                } else {
                    withStyle(SpanStyle(color = defaultColor)) { append(word) }
                }
                i = j
                continue
            }
            withStyle(SpanStyle(color = defaultColor)) { append(ch) }
            i++
        }
    }
}