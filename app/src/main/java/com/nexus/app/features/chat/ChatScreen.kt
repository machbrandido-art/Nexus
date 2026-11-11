package com.nexus.app.features.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.app.R
import com.nexus.app.modes.Mode
import com.nexus.app.modes.PresetModes
import com.nexus.app.providers.ChatMessage
import com.nexus.app.providers.ProviderConfig
import com.nexus.app.providers.compat.NimPreset
import com.nexus.app.ui.components.ErrorBanner
import com.nexus.app.ui.components.MessageBubble
import com.nexus.app.ui.components.SimpleMarkdown
import com.nexus.app.ui.components.StreamingIndicator
import com.nexus.app.ui.navigation.LocalDrawerController
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onOpenProviders: () -> Unit,
    viewModel: ChatViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerController = LocalDrawerController.current

    var showModeMenu by remember { mutableStateOf(false) }
    var showModelSheet by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val modes: List<Mode> = remember { PresetModes }
    val currentMode = modes.firstOrNull { it.id == state.modeId } ?: modes.first()

    LaunchedEffect(state.messages.size, state.streamingText) {
        val target = listState.layoutInfo.totalItemsCount - 1
        if (target >= 0) listState.animateScrollToItem(target)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { drawerController.openDrawer() }) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.cd_open_drawer)
                        )
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Box {
                            AssistChip(
                                onClick = { showModeMenu = true },
                                label = {
                                    Text(
                                        text = stringResource(currentMode.nameResId),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        currentMode.icon,
                                        contentDescription = stringResource(R.string.cd_mode_switch),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                            DropdownMenu(
                                expanded = showModeMenu,
                                onDismissRequest = { showModeMenu = false }
                            ) {
                                modes.forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(mode.nameResId)) },
                                        leadingIcon = {
                                            Icon(mode.icon, contentDescription = null)
                                        },
                                        trailingIcon = if (mode.id == currentMode.id) {
                                            {
                                                Icon(
                                                    Icons.Filled.Check,
                                                    contentDescription = null
                                                )
                                            }
                                        } else null,
                                        onClick = {
                                            viewModel.switchMode(mode.id)
                                            showModeMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    if (state.conversationId != null) {
                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.chat_topbar_menu)
                            )
                        }
                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_menu_rename)) },
                                leadingIcon = {
                                    Icon(Icons.Filled.Edit, contentDescription = null)
                                },
                                onClick = {
                                    showRenameDialog = true
                                    showTopMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_menu_delete)) },
                                leadingIcon = {
                                    Icon(Icons.Filled.Delete, contentDescription = null)
                                },
                                onClick = {
                                    showDeleteDialog = true
                                    showTopMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            if (state.providers.isEmpty()) {
                EmptyProvidersPrompt(onOpenProviders = onOpenProviders)
                return@Column
            }

            Box(modifier = Modifier.weight(1f)) {
                if (state.messages.isEmpty() && state.streamingText.isEmpty()) {
                    EmptyChatState()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(items = state.messages, key = { it.id }) { msg ->
                            MessageBubble(
                                message = ChatMessage(msg.role, msg.content),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (state.streamingText.isNotEmpty() || state.streamingReasoning.isNotEmpty()) {
                            item(key = "__streaming__") {
                                if (state.streamingReasoning.isNotEmpty()) {
                                    ReasoningBlock(state.streamingReasoning)
                                }
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        if (state.streamingText.isNotEmpty()) {
                                            SimpleMarkdown(text = state.streamingText)
                                        } else {
                                            StreamingIndicator()
                                        }
                                    }
                                }
                            }
                        } else if (state.isStreaming) {
                            item(key = "__thinking__") { StreamingIndicator() }
                        }
                    }
                }
            }

            if (state.error != null) {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ErrorBanner(
                        message = state.error!!,
                        onRetry = { viewModel.clearError() }
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
                onOpenModelPicker = { showModelSheet = true }
            )
        }
    }

    if (showModelSheet) {
        ModelSheet(
            providers = state.providers,
            selectedProviderId = state.selectedProviderId,
            selectedModel = state.selectedModel,
            onSelect = { pid, mid ->
                viewModel.selectProviderModel(pid, mid)
                showModelSheet = false
            },
            onDismiss = { showModelSheet = false }
        )
    }

    if (showRenameDialog) {
        val convId = state.conversationId
        var newTitle by remember(convId) { mutableStateOf(state.title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.chat_menu_rename)) },
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
                    if (convId != null) viewModel.renameConversation(convId, newTitle)
                    showRenameDialog = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showDeleteDialog) {
        val convId = state.conversationId
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.chat_menu_delete)) },
            text = { Text(stringResource(R.string.providers_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    if (convId != null) viewModel.deleteConversation(convId)
                    showDeleteDialog = false
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun EmptyChatState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.chat_empty_title),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.chat_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyProvidersPrompt(onOpenProviders: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.error_no_provider),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onOpenProviders) {
                Text(stringResource(R.string.providers_add))
            }
        }
    }
}

@Composable
private fun Composer(
    input: String,
    isStreaming: Boolean,
    currentModel: String?,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onOpenModelPicker: () -> Unit
) {
    Surface(tonalElevation = 2.dp) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = onOpenModelPicker,
                    label = {
                        Text(
                            text = currentModel ?: stringResource(R.string.chat_switch_model),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Cloud,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Filled.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.chat_input_hint)) },
                    maxLines = 6,
                    shape = RoundedCornerShape(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = { if (isStreaming) onStop() else onSend() },
                    modifier = Modifier.size(48.dp)
                ) {
                    if (isStreaming) {
                        Icon(
                            Icons.Filled.Stop,
                            contentDescription = stringResource(R.string.action_stop)
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.action_send)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReasoningBlock(text: String) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.chat_reasoning),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (expanded) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSheet(
    providers: List<ProviderConfig>,
    selectedProviderId: String?,
    selectedModel: String?,
    onSelect: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.85f)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.chat_switch_model),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.nim_model_search_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(24.dp)
            )

            LazyColumn(modifier = Modifier.weight(1f)) {
                providers.forEach { provider ->
                    val filtered = provider.models.filter {
                        search.isBlank() || it.contains(search, ignoreCase = true)
                    }
                    if (filtered.isEmpty()) return@forEach

                    item(key = "hdr-${provider.id}") {
                        Text(
                            text = provider.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(
                                start = 20.dp,
                                end = 20.dp,
                                top = 12.dp,
                                bottom = 4.dp
                            )
                        )
                    }

                    if (provider.presetId == NimPreset.ID) {
                        val grouped = NimPreset.groupModelsByVendor(filtered)
                        grouped.forEach { (vendor, models) ->
                            item(key = "vendor-${provider.id}-$vendor") {
                                Text(
                                    text = vendor,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(
                                        start = 32.dp,
                                        end = 20.dp,
                                        top = 6.dp,
                                        bottom = 2.dp
                                    )
                                )
                            }
                            items(models, key = { "m-${provider.id}-$it" }) { model ->
                                ModelRow(
                                    model = model,
                                    isSelected = provider.id == selectedProviderId && model == selectedModel,
                                    onClick = { onSelect(provider.id, model) }
                                )
                            }
                        }
                    } else {
                        items(filtered, key = { "m-${provider.id}-$it" }) { model ->
                            ModelRow(
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
private fun ModelRow(
    model: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = model,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}