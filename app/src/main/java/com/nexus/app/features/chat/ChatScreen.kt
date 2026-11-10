package com.nexus.app.features.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.app.R
import com.nexus.app.modes.Mode
import com.nexus.app.modes.PresetModes
import com.nexus.app.providers.ChatMessage
import com.nexus.app.providers.ProviderConfig
import com.nexus.app.ui.components.ErrorBanner
import com.nexus.app.ui.components.MessageBubble
import com.nexus.app.ui.components.ModeChip
import com.nexus.app.ui.components.SimpleMarkdown
import com.nexus.app.ui.components.StreamingIndicator
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onOpenProviders: () -> Unit,
    viewModel: ChatViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showModeSheet by remember { mutableStateOf(false) }
    var showModelSheet by remember { mutableStateOf(false) }
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
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ModeChip(
                            icon = currentMode.icon,
                            label = stringResource(currentMode.nameResId),
                            onClick = { showModeSheet = true }
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            onClick = { showModelSheet = true },
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Cloud,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = state.selectedModel ?: "Pick a model",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    Icons.Filled.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::newChat) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.chat_new))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
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
                onInputChange = viewModel::updateInput,
                onSend = viewModel::send,
                onStop = viewModel::cancelStreaming
            )
        }
    }

    if (showModeSheet) {
        ModeSheet(
            modes = modes,
            selectedId = state.modeId,
            onSelect = {
                viewModel.switchMode(it.id)
                showModeSheet = false
            },
            onDismiss = { showModeSheet = false }
        )
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
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    Surface(tonalElevation = 2.dp) {
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
                    Icon(Icons.Filled.Stop, contentDescription = stringResource(R.string.action_stop))
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
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(24.dp)) {
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
private fun ModeSheet(
    modes: List<Mode>,
    selectedId: String,
    onSelect: (Mode) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = stringResource(R.string.chat_mode_label),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            modes.forEach { mode ->
                Surface(
                    onClick = { onSelect(mode) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(mode.nameResId)) },
                        supportingContent = { Text(mode.systemPrompt.take(70) + "…") },
                        leadingContent = { Icon(mode.icon, contentDescription = null) },
                        trailingContent = if (mode.id == selectedId) {
                            { Text("✓", style = MaterialTheme.typography.titleLarge) }
                        } else null
                    )
                }
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = stringResource(R.string.chat_switch_model),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            providers.forEach { provider ->
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                if (provider.models.isEmpty()) {
                    Text(
                        text = "(no models configured)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                provider.models.forEach { model ->
                    val isSelected = provider.id == selectedProviderId && model == selectedModel
                    Surface(
                        onClick = { onSelect(provider.id, model) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = model,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Text("✓", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}