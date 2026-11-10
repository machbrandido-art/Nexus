package com.nexus.app.modes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.ui.graphics.vector.ImageVector
import com.nexus.app.R

enum class ModeLayout { CHAT, WORK, CODE, RESEARCH }

data class Mode(
    val id: String,
    val nameResId: Int,
    val icon: ImageVector,
    val systemPrompt: String,
    val defaultModel: String? = null,
    val tools: List<String> = emptyList(),
    val layout: ModeLayout = ModeLayout.CHAT,
    val temperature: Float? = null
)

val PresetModes: List<Mode> = listOf(
    Mode(
        id = "chat",
        nameResId = R.string.mode_chat,
        icon = Icons.AutoMirrored.Filled.Chat,
        systemPrompt = "You are a thoughtful, helpful assistant. Be clear, honest about uncertainty, and never invent facts.",
        layout = ModeLayout.CHAT,
        temperature = 0.7f
    ),
    Mode(
        id = "work",
        nameResId = R.string.mode_work,
        icon = Icons.Filled.ViewKanban,
        systemPrompt = "You are a task-oriented assistant. Turn answers into structured plans: numbered tasks, owners, and acceptance criteria. Use tables when comparing options.",
        layout = ModeLayout.WORK,
        temperature = 0.4f
    ),
    Mode(
        id = "code",
        nameResId = R.string.mode_code,
        icon = Icons.Filled.Code,
        systemPrompt = "You are an expert software engineer. Provide runnable code with clear file paths. Explain briefly, favor correctness over cleverness, and always note assumptions.",
        layout = ModeLayout.CODE,
        temperature = 0.2f
    ),
    Mode(
        id = "research",
        nameResId = R.string.mode_research,
        icon = Icons.Filled.Science,
        systemPrompt = "You are a research assistant. Reason step by step, cite sources when available, and clearly label uncertainty. Produce long-form, well-structured reports.",
        layout = ModeLayout.RESEARCH,
        temperature = 0.5f
    )
)