package com.nexus.app.ui.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewKanban
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nexus.app.R
import com.nexus.app.features.chat.ConversationSummary
import com.nexus.app.modes.PresetModes
import com.nexus.app.providers.ProviderConfig
import com.nexus.app.ui.components.DrawerItem
import com.nexus.app.ui.components.NexusButton
import com.nexus.app.ui.components.NexusButtonVariant
import com.nexus.app.ui.components.NexusMenu
import com.nexus.app.ui.components.NexusMenuItem
import com.nexus.app.ui.components.NexusTextField
import com.nexus.app.ui.theme.LocalNexusColors
import com.nexus.app.ui.theme.LocalNexusSpacing
import com.nexus.app.ui.theme.LocalNexusTypography

data class DrawerUiState(
    val recentConversations: List<ConversationSummary> = emptyList(),
    val activeModeId: String = "chat",
    val activeProvider: ProviderConfig? = null,
    val activeModel: String? = null,
    val activeConversationId: String? = null,
    val currentRoute: String? = null
)

@Composable
fun AppDrawerContent(
    state: DrawerUiState,
    onNewChat: () -> Unit,
    onModeSelected: (String) -> Unit,
    onAddMode: () -> Unit,
    onConversationSelected: (String) -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onPinConversation: (String, Boolean) -> Unit,
    onArchiveConversation: (String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onActiveProviderTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalNexusColors.current
    val spacing = LocalNexusSpacing.current

    var search by remember { mutableStateOf("") }
    var actionTarget by remember { mutableStateOf<ConversationSummary?>(null) }
    var renameTarget by remember { mutableStateOf<ConversationSummary?>(null) }
    var deleteTarget by remember { mutableStateOf<ConversationSummary?>(null) }

    val filtered = remember(state.recentConversations, search) {
        val q = search.trim()
        if (q.isBlank()) state.recentConversations
        else state.recentConversations.filter {
            it.title.contains(q, ignoreCase = true) ||
                (it.model?.contains(q, ignoreCase = true) == true)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxHeight()
            .background(c.bg)
    ) {
        item {
            LimeHeader(onNewChat = onNewChat)
        }

        item {
            Column(modifier = Modifier.padding(vertical = spacing.xs)) {
                BasicText(
                    text = stringResource(R.string.drawer_modes_header),
                    style = LocalNexusTypography.current.caption.copy(color = c.textMuted),
                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs)
                )
                PresetModes.forEach { mode ->
                    DrawerItem(
                        icon = mode.icon,
                        label = stringResource(mode.nameResId),
                        selected = state.activeModeId == mode.id,
                        onClick = { onModeSelected(mode.id) }
                    )
                }
                DrawerItem(
                    icon = Icons.Filled.Add,
                    label = stringResource(R.string.drawer_add_mode),
                    selected = false,
                    onClick = onAddMode
                )
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs)) {
                BasicText(
                    text = stringResource(R.string.drawer_recent_header),
                    style = LocalNexusTypography.current.caption.copy(color = c.textMuted),
                    modifier = Modifier.padding(vertical = spacing.xs)
                )
                NexusTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = stringResource(R.string.drawer_search_chats_hint),
                    singleLine = true
                )
            }
        }

        if (filtered.isEmpty()) {
            item {
                BasicText(
                    text = stringResource(R.string.drawer_no_conversations),
                    style = LocalNexusTypography.current.caption.copy(color = c.textMuted),
                    modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm)
                )
            }
        } else {
            items(items = filtered, key = { it.id }) { conv ->
                RecentRow(
                    conv = conv,
                    selected = conv.id == state.activeConversationId && state.currentRoute == "chat",
                    onClick = { onConversationSelected(conv.id) },
                    onLongClick = { actionTarget = conv }
                )
            }
        }

        item {
            Column(modifier = Modifier.padding(vertical = spacing.xs)) {
                DrawerItem(
                    icon = Icons.Filled.Cloud,
                    label = stringResource(R.string.nav_providers),
                    selected = state.currentRoute == "providers",
                    onClick = { onNavigate("providers") }
                )
                DrawerItem(
                    icon = Icons.Filled.ViewKanban,
                    label = stringResource(R.string.nav_kanban),
                    selected = state.currentRoute == "kanban",
                    onClick = { onNavigate("kanban") }
                )
                DrawerItem(
                    icon = Icons.Filled.Settings,
                    label = stringResource(R.string.nav_settings),
                    selected = state.currentRoute == "settings",
                    onClick = { onNavigate("settings") }
                )
            }
        }

        item {
            ProviderChip(
                provider = state.activeProvider,
                model = state.activeModel,
                onClick = onActiveProviderTap
            )
        }
    }

    val target = actionTarget
    if (target != null) {
        Box(modifier = Modifier.fillMaxWidth()) {
            NexusMenu(
                expanded = true,
                onDismissRequest = { actionTarget = null },
                alignment = Alignment.Center
            ) {
                NexusMenuItem(
                    text = stringResource(R.string.drawer_rename),
                    icon = Icons.Filled.Edit,
                    onClick = { renameTarget = target; actionTarget = null }
                )
                NexusMenuItem(
                    text = stringResource(
                        if (target.pinned) R.string.drawer_unpin else R.string.drawer_pin
                    ),
                    icon = Icons.Filled.PushPin,
                    onClick = {
                        onPinConversation(target.id, !target.pinned)
                        actionTarget = null
                    }
                )
                NexusMenuItem(
                    text = stringResource(R.string.drawer_archive),
                    icon = Icons.Filled.Archive,
                    onClick = {
                        onArchiveConversation(target.id)
                        actionTarget = null
                    }
                )
                NexusMenuItem(
                    text = stringResource(R.string.drawer_delete_chat),
                    icon = Icons.Filled.Delete,
                    onClick = { deleteTarget = target; actionTarget = null }
                )
            }
        }
    }

    val rename = renameTarget
    if (rename != null) {
        var newTitle by remember(rename.id) { mutableStateOf(rename.title) }
        Dialog(onDismissRequest = { renameTarget = null }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(c.bg)
                    .padding(spacing.lg)
            ) {
                BasicText(
                    text = stringResource(R.string.action_rename),
                    style = LocalNexusTypography.current.title.copy(color = c.text)
                )
                Spacer(Modifier.height(spacing.md))
                NexusTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true
                )
                Spacer(Modifier.height(spacing.lg))
                Row {
                    NexusButton(
                        text = stringResource(R.string.action_cancel),
                        onClick = { renameTarget = null },
                        variant = NexusButtonVariant.Surface,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(spacing.xs))
                    NexusButton(
                        text = stringResource(R.string.action_save),
                        onClick = {
                            onRenameConversation(rename.id, newTitle)
                            renameTarget = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    val del = deleteTarget
    if (del != null) {
        Dialog(onDismissRequest = { deleteTarget = null }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(c.bg)
                    .padding(spacing.lg)
            ) {
                BasicText(
                    text = stringResource(R.string.drawer_delete_chat),
                    style = LocalNexusTypography.current.title.copy(color = c.text)
                )
                Spacer(Modifier.height(spacing.sm))
                BasicText(
                    text = stringResource(R.string.providers_delete_confirm_body),
                    style = LocalNexusTypography.current.body.copy(color = c.textMuted)
                )
                Spacer(Modifier.height(spacing.lg))
                Row {
                    NexusButton(
                        text = stringResource(R.string.action_cancel),
                        onClick = { deleteTarget = null },
                        variant = NexusButtonVariant.Surface,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(spacing.xs))
                    NexusButton(
                        text = stringResource(R.string.action_delete),
                        onClick = {
                            onDeleteConversation(del.id)
                            deleteTarget = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LimeHeader(onNewChat: () -> Unit) {
    val c = LocalNexusColors.current
    val type = LocalNexusTypography.current
    val spacing = LocalNexusSpacing.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(c.lime)
            .padding(spacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(c.ink),
                contentAlignment = Alignment.Center
            ) {
                BasicText(text = "N", style = type.title.copy(color = c.lime))
            }
            Spacer(Modifier.width(spacing.sm))
            Column {
                BasicText(
                    text = stringResource(R.string.app_name),
                    style = type.title.copy(color = c.ink)
                )
                BasicText(
                    text = stringResource(R.string.app_tagline),
                    style = type.caption.copy(color = c.ink.copy(alpha = 0.7f)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(spacing.md))
        NexusButton(
            text = "+ " + stringResource(R.string.drawer_new_chat),
            onClick = onNewChat,
            variant = NexusButtonVariant.Ink,
            fullWidth = true
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentRow(
    conv: ConversationSummary,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val c = LocalNexusColors.current
    val type = LocalNexusTypography.current
    val spacing = LocalNexusSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.sm, vertical = 2.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) c.lime else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = spacing.sm, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Chat,
            contentDescription = null,
            tint = if (selected) c.ink else c.textMuted,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(spacing.sm))
        BasicText(
            text = conv.title,
            style = type.body.copy(color = if (selected) c.ink else c.textMuted),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (conv.pinned) {
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = null,
                tint = if (selected) c.ink else c.textMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun ProviderChip(
    provider: ProviderConfig?,
    model: String?,
    onClick: () -> Unit
) {
    val c = LocalNexusColors.current
    val type = LocalNexusTypography.current
    val spacing = LocalNexusSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(spacing.md)
            .clip(RoundedCornerShape(20.dp))
            .background(c.surface)
            .clickable(onClick = onClick)
            .padding(spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(c.lime),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Cloud,
                contentDescription = null,
                tint = c.ink,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = provider?.name ?: stringResource(R.string.drawer_active_provider_label),
                style = type.body.copy(color = c.text),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            BasicText(
                text = model ?: "—",
                style = type.caption.copy(color = c.textMuted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = c.textMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}