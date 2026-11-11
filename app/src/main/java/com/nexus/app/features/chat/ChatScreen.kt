package com.nexus.app.features.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.app.R
import com.nexus.app.modes.Mode
import com.nexus.app.modes.PresetModes
import com.nexus.app.providers.ChatMessage
import com.nexus.app.providers.ProviderConfig
import com.nexus.app.providers.compat.NimPreset
import com.nexus.app.ui.components.ChatBubble
import com.nexus.app.ui.components.CircleIconButton
import com.nexus.app.ui.components.Composer
import com.nexus.app.ui.components.EmptyState
import com.nexus.app.ui.components.ModeChip
import com.nexus.app.ui.components.NexusButton
import com.nexus.app.ui.components.NexusButtonVariant
import com.nexus.app.ui.components.NexusMenu
import com.nexus.app.ui.components.NexusMenuItem
import com.nexus.app.ui.components.NexusTextField
import com.nexus.app.ui.navigation.LocalDrawerController
import com.nexus.app.ui.theme.LocalNexusColors
import com.nexus.app.ui.theme.LocalNexusSpacing
import com.nexus.app.ui.theme.LocalNexusTypography
import org.koin.androidx.compose.koinViewModel

@Composable
fun ChatScreen(
    onOpenProviders: () -> Unit,
    viewModel: ChatViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val drawer = LocalDrawerController.current
    val c = LocalNexusColors.current
    val type = LocalNexusTypography.current
    val spacing = LocalNexusSpacing.current

    var showModeMenu by remember { mutableStateOf(false) }
    var showOverflow by remember { mutableStateOf(false) }
    var showModelPicker by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val modes: List<Mode> = remember { PresetModes }
    val currentMode = modes.firstOrNull { it.id == state.modeId } ?: modes.first()

    LaunchedEffect(state.messages.size, state.streamingText) {
        val target = listState.layoutInfo.totalItemsCount - 1
        if (target >= 0) listState.animateScrollToItem(target)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.sm, vertical = spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIconButton(
                icon = Icons.Filled.Menu,
                contentDescription = stringResource(R.string.cd_open_drawer),
                onClick = { drawer.openDrawer() }
            )
            Spacer(Modifier.width(spacing.xs))
            BasicText(
                text = state.title,
                style = type.subtitle.copy(color = c.text),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box {
                ModeChip(
                    icon = currentMode.icon,
                    label = stringResource(currentMode.nameResId),
                    onClick = { showModeMenu = true }
                )
                NexusMenu(
                    expanded = showModeMenu,
                    onDismissRequest = { showModeMenu = false },
                    alignment = Alignment.TopEnd,
                    offset = IntOffset(0, 44)
                ) {
                    modes.forEach { mode ->
                        NexusMenuItem(
                            text = stringResource(mode.nameResId),
                            icon = mode.icon,
                            selected = mode.id == currentMode.id,
                            onClick = {
                                viewModel.switchMode(mode.id)
                                showModeMenu = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.width(spacing.xs))
            Box {
                CircleIconButton(
                    icon = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.chat_topbar_menu),
                    onClick = { showOverflow = true },
                    variant = NexusButtonVariant.Surface
                )
                NexusMenu(
                    expanded = showOverflow,
                    onDismissRequest = { showOverflow = false },
                    alignment = Alignment.TopEnd,
                    offset = IntOffset(0, 44)
                ) {
                    if (state.conversationId != null) {
                        NexusMenuItem(
                            text = stringResource(R.string.chat_menu_rename),
                            icon = Icons.Filled.Edit,
                            onClick = { showRename = true; showOverflow = false }
                        )
                        NexusMenuItem(
                            text = stringResource(R.string.chat_menu_delete),
                            icon = Icons.Filled.Delete,
                            onClick = { showDelete = true; showOverflow = false }
                        )
                    }
                }
            }
        }

        if (state.providers.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EmptyState(
                        title = stringResource(R.string.error_no_provider),
                        subtitle = stringResource(R.string.chat_empty_body)
                    )
                    Spacer(Modifier.height(spacing.md))
                    NexusButton(
                        text = stringResource(R.string.providers_add),
                        onClick = onOpenProviders,
                        variant = NexusButtonVariant.Ink
                    )
                }
            }
            return@Column
        }

        Box(modifier = Modifier.weight(1f)) {
            if (state.messages.isEmpty() && state.streamingText.isEmpty() && state.streamingReasoning.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        title = stringResource(R.string.chat_empty_title),
                        subtitle = stringResource(R.string.chat_empty_body)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = spacing.md, vertical = spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    items(items = state.messages, key = { it.id }) { msg ->
                        ChatBubble(
                            message = ChatMessage(msg.role, msg.content),
                            reasoning = msg.reasoning
                        )
                    }
                    if (state.streamingText.isNotEmpty() || state.streamingReasoning.isNotEmpty()) {
                        item(key = "__streaming__") {
                            ChatBubble(
                                message = ChatMessage(ChatMessage.Role.ASSISTANT, state.streamingText),
                                reasoning = state.streamingReasoning.ifBlank { null }
                            )
                        }
                    } else if (state.isStreaming) {
                        item(key = "__thinking__") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(c.surface)
                                    .padding(spacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicText(
                                    text = "Thinking…",
                                    style = type.body.copy(color = c.textMuted)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (state.error != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.md, vertical = spacing.xs)
                    .clip(RoundedCornerShape(20.dp))
                    .background(c.error)
                    .padding(horizontal = spacing.md, vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = state.error!!,
                    style = type.body.copy(color = c.onInk),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(spacing.xs))
                BasicText(
                    text = "Dismiss",
                    style = type.caption.copy(color = c.onInk),
                    modifier = Modifier.clickable { viewModel.clearError() }
                )
            }
        }

        Composer(
            input = state.input,
            isStreaming = state.isStreaming,
            currentModel = state.selectedModel,
            onInputChange = viewModel::updateInput,
            onSend = viewModel::send,
            onStop = viewModel::cancelStreaming,
            onOpenModelPicker = { showModelPicker = true }
        )
    }

    if (showModelPicker) {
        ModelPickerDialog(
            providers = state.providers,
            selectedProviderId = state.selectedProviderId,
            selectedModel = state.selectedModel,
            onSelect = { pid, mid ->
                viewModel.selectProviderModel(pid, mid)
                showModelPicker = false
            },
            onDismiss = { showModelPicker = false }
        )
    }

    if (showRename) {
        val convId = state.conversationId
        var newTitle by remember(convId) { mutableStateOf(state.title) }
        RenameDialog(
            initial = newTitle,
            onValueChange = { newTitle = it },
            onConfirm = {
                if (convId != null) viewModel.renameConversation(convId, newTitle)
                showRename = false
            },
            onDismiss = { showRename = false }
        )
    }

    if (showDelete) {
        val convId = state.conversationId
        ConfirmDeleteDialog(
            title = stringResource(R.string.chat_menu_delete),
            body = stringResource(R.string.providers_delete_confirm_body),
            onConfirm = {
                if (convId != null) viewModel.deleteConversation(convId)
                showDelete = false
            },
            onDismiss = { showDelete = false }
        )
    }
}

@Composable
private fun ModelPickerDialog(
    providers: List<ProviderConfig>,
    selectedProviderId: String?,
    selectedModel: String?,
    onSelect: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalNexusColors.current
    val type = LocalNexusTypography.current
    val spacing = LocalNexusSpacing.current
    var search by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(c.bg)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(spacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = stringResource(R.string.chat_switch_model),
                    style = type.title.copy(color = c.text),
                    modifier = Modifier.weight(1f)
                )
                CircleIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.action_close),
                    onClick = onDismiss,
                    variant = NexusButtonVariant.Surface
                )
            }
            Spacer(Modifier.height(spacing.sm))
            NexusTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = stringResource(R.string.nim_model_search_hint),
                singleLine = true
            )
            Spacer(Modifier.height(spacing.sm))
            LazyColumn(modifier = Modifier.weight(1f)) {
                providers.forEach { provider ->
                    val filtered = provider.models.filter {
                        search.isBlank() || it.contains(search, ignoreCase = true)
                    }
                    if (filtered.isEmpty()) return@forEach

                    item(key = "hdr-${provider.id}") {
                        BasicText(
                            text = provider.name,
                            style = type.caption.copy(color = c.limeDeep),
                            modifier = Modifier.padding(vertical = spacing.xs)
                        )
                    }
                    if (provider.presetId == NimPreset.ID) {
                        val grouped = NimPreset.groupModelsByVendor(filtered)
                        grouped.forEach { (vendor, models) ->
                            item(key = "v-${provider.id}-$vendor") {
                                BasicText(
                                    text = vendor,
                                    style = type.caption.copy(color = c.textMuted),
                                    modifier = Modifier.padding(
                                        start = spacing.sm,
                                        top = spacing.xs,
                                        bottom = 2.dp
                                    )
                                )
                            }
                            items(models, key = { "m-${provider.id}-$it" }) { model ->
                                ModelPickRow(
                                    model = model,
                                    isSelected = provider.id == selectedProviderId && model == selectedModel,
                                    onClick = { onSelect(provider.id, model) }
                                )
                            }
                        }
                    } else {
                        items(filtered, key = { "m-${provider.id}-$it" }) { model ->
                            ModelPickRow(
                                model = model,
                                isSelected = provider.id == selectedProviderId && model == selectedModel,
                                onClick = { onSelect(provider.id, model) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelPickRow(
    model: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val c = LocalNexusColors.current
    val type = LocalNexusTypography.current
    val spacing = LocalNexusSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) c.lime else c.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            text = model,
            style = type.body.copy(color = if (isSelected) c.ink else c.text),
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = c.ink)
        }
    }
}

@Composable
private fun RenameDialog(
    initial: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalNexusColors.current
    val type = LocalNexusTypography.current
    val spacing = LocalNexusSpacing.current
    var value by remember { mutableStateOf(initial) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(c.bg)
                .padding(spacing.lg)
        ) {
            BasicText(
                text = stringResource(R.string.action_rename),
                style = type.title.copy(color = c.text)
            )
            Spacer(Modifier.height(spacing.md))
            NexusTextField(
                value = value,
                onValueChange = {
                    value = it
                    onValueChange(it)
                },
                singleLine = true
            )
            Spacer(Modifier.height(spacing.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                NexusButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onDismiss,
                    variant = NexusButtonVariant.Surface,
                    modifier = Modifier.weight(1f)
                )
                NexusButton(
                    text = stringResource(R.string.action_save),
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    body: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalNexusColors.current
    val type = LocalNexusTypography.current
    val spacing = LocalNexusSpacing.current

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(c.bg)
                .padding(spacing.lg)
        ) {
            BasicText(text = title, style = type.title.copy(color = c.text))
            Spacer(Modifier.height(spacing.sm))
            BasicText(text = body, style = type.body.copy(color = c.textMuted))
            Spacer(Modifier.height(spacing.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                NexusButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onDismiss,
                    variant = NexusButtonVariant.Surface,
                    modifier = Modifier.weight(1f)
                )
                NexusButton(
                    text = stringResource(R.string.action_delete),
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}