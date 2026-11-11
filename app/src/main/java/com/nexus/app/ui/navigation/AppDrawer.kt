package com.nexus.app.ui.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.app.R
import com.nexus.app.features.chat.ConversationSummary
import com.nexus.app.modes.PresetModes
import com.nexus.app.providers.ProviderConfig

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

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))
            ExtendedFloatingActionButton(
                onClick = onNewChat,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.drawer_new_chat)) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Divider(modifier = Modifier.padding(vertical = 4.dp))

        Text(
            text = stringResource(R.string.drawer_modes_header),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
        )

        PresetModes.forEach { mode ->
            NavigationDrawerItem(
                label = { Text(stringResource(mode.nameResId)) },
                icon = { Icon(mode.icon, contentDescription = null) },
                selected = state.activeModeId == mode.id,
                onClick = { onModeSelected(mode.id) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.drawer_add_mode)) },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            selected = false,
            onClick = onAddMode,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        Spacer(Modifier.height(8.dp))
        Divider(modifier = Modifier.padding(vertical = 4.dp))

        Text(
            text = stringResource(R.string.drawer_recent_header),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp)
        )
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text(stringResource(R.string.drawer_search_chats_hint)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(24.dp)
        )

        if (filtered.isEmpty()) {
            Text(
                text = stringResource(R.string.drawer_no_conversations),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(items = filtered, key = { it.id }) { conv ->
                    ConversationRow(
                        conv = conv,
                        selected = conv.id == state.activeConversationId && state.currentRoute == "chat",
                        onClick = { onConversationSelected(conv.id) },
                        onLongClick = { actionTarget = conv }
                    )
                }
            }
        }

        Divider()
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.nav_providers)) },
            icon = { Icon(Icons.Filled.Cloud, contentDescription = null) },
            selected = state.currentRoute == "providers",
            onClick = { onNavigate("providers") },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.nav_kanban)) },
            icon = { Icon(Icons.Filled.ViewKanban, contentDescription = null) },
            selected = state.currentRoute == "kanban",
            onClick = { onNavigate("kanban") },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.nav_settings)) },
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            selected = state.currentRoute == "settings",
            onClick = { onNavigate("settings") },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        Divider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onActiveProviderTap)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.activeProvider?.name ?: stringResource(R.string.drawer_active_provider_label),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = state.activeModel ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Filled.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    val target = actionTarget
    if (target != null) {
        DropdownMenu(
            expanded = true,
            onDismissRequest = { actionTarget = null }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.drawer_rename)) },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                onClick = { renameTarget = target; actionTarget = null }
            )
            DropdownMenuItem(
                text = {
                    Text(stringResource(if (target.pinned) R.string.drawer_unpin else R.string.drawer_pin))
                },
                leadingIcon = { Icon(Icons.Filled.PushPin, contentDescription = null) },
                onClick = {
                    onPinConversation(target.id, !target.pinned)
                    actionTarget = null
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.drawer_archive)) },
                leadingIcon = { Icon(Icons.Filled.Archive, contentDescription = null) },
                onClick = {
                    onArchiveConversation(target.id)
                    actionTarget = null
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.drawer_delete_chat)) },
                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                onClick = { deleteTarget = target; actionTarget = null }
            )
        }
    }

    val rename = renameTarget
    if (rename != null) {
        var newTitle by remember(rename.id) { mutableStateOf(rename.title) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(stringResource(R.string.action_rename)) },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.providers_name)) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRenameConversation(rename.id, newTitle)
                    renameTarget = null
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    val del = deleteTarget
    if (del != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.drawer_delete_chat)) },
            text = { Text(stringResource(R.string.providers_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteConversation(del.id)
                    deleteTarget = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationRow(
    conv: ConversationSummary,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 28.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (conv.pinned) {
            Icon(
                Icons.Filled.PushPin,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = conv.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AppNavigationRail(
    state: DrawerUiState,
    onNavigate: (String) -> Unit,
    onNewChat: () -> Unit
) {
    NavigationRail {
        Spacer(Modifier.height(8.dp))
        NavigationRailItem(
            icon = { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.drawer_new_chat)) },
            label = { Text(stringResource(R.string.drawer_new_chat)) },
            selected = false,
            onClick = onNewChat
        )
        Spacer(Modifier.height(8.dp))
        NavigationRailItem(
            icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_chat)) },
            selected = state.currentRoute == "chat",
            onClick = { onNavigate("chat") }
        )
        NavigationRailItem(
            icon = { Icon(Icons.Filled.ViewKanban, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_kanban)) },
            selected = state.currentRoute == "kanban",
            onClick = { onNavigate("kanban") }
        )
        NavigationRailItem(
            icon = { Icon(Icons.Filled.Cloud, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_providers)) },
            selected = state.currentRoute == "providers",
            onClick = { onNavigate("providers") }
        )
        NavigationRailItem(
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_settings)) },
            selected = state.currentRoute == "settings",
            onClick = { onNavigate("settings") }
        )
    }
}