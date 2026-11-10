package com.nexus.app.features.providers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.app.R
import com.nexus.app.providers.ProviderConfig
import com.nexus.app.providers.ProviderType
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvidersScreen(viewModel: ProvidersViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var deleteTarget by remember { mutableStateOf<ProviderConfig?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.providers_title)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::startAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.providers_add)) }
            )
        }
    ) { padding ->
        if (state.providers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.providers_empty),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = viewModel::startAdd) {
                        Text(stringResource(R.string.providers_add))
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(items = state.providers, key = { it.id }) { provider ->
                    ProviderRow(
                        provider = provider,
                        onEdit = { viewModel.startEdit(provider) },
                        onDuplicate = { viewModel.duplicate(provider) },
                        onDelete = { deleteTarget = provider }
                    )
                    Divider()
                }
            }
        }
    }

    if (state.editing != null) {
        ProviderEditDialog(
            form = state.editing!!,
            isTesting = state.isTesting,
            isFetchingModels = state.isFetchingModels,
            testResult = state.testResult,
            info = state.info,
            error = state.error,
            onUpdate = viewModel::updateForm,
            onTypeChange = viewModel::onTypeChange,
            onTest = viewModel::testConnection,
            onFetchModels = viewModel::fetchModels,
            onSave = viewModel::save,
            onCancel = viewModel::cancelEdit
        )
    }

    val target = deleteTarget
    if (target != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.providers_delete_confirm)) },
            text = { Text(stringResource(R.string.providers_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(target)
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

@Composable
private fun ProviderRow(
    provider: ProviderConfig,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${provider.type.name} · ${provider.models.size} models",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDuplicate) {
                Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.action_duplicate))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
            }
            Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ProviderEditDialog(
    form: ProviderForm,
    isTesting: Boolean,
    isFetchingModels: Boolean,
    testResult: String?,
    info: String?,
    error: String?,
    onUpdate: ((ProviderForm) -> ProviderForm) -> Unit,
    onTypeChange: (ProviderType) -> Unit,
    onTest: () -> Unit,
    onFetchModels: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    var typeMenuOpen by remember { mutableStateOf(false) }
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(
                        if (form.id == null) R.string.providers_add else R.string.providers_edit_title
                    ),
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = form.name,
                    onValueChange = { v -> onUpdate { it.copy(name = v) } },
                    label = { Text(stringResource(R.string.providers_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                Box {
                    OutlinedButton(
                        onClick = { typeMenuOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(typeLabel(form.type)),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    DropdownMenu(
                        expanded = typeMenuOpen,
                        onDismissRequest = { typeMenuOpen = false }
                    ) {
                        ProviderType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(stringResource(typeLabel(type))) },
                                onClick = {
                                    onTypeChange(type)
                                    typeMenuOpen = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = form.baseUrl,
                    onValueChange = { v -> onUpdate { it.copy(baseUrl = v) } },
                    label = { Text(stringResource(R.string.providers_base_url)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = form.apiKey,
                    onValueChange = { v -> onUpdate { it.copy(apiKey = v) } },
                    label = { Text(stringResource(R.string.providers_api_key)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = form.modelsCsv,
                    onValueChange = { v -> onUpdate { it.copy(modelsCsv = v) } },
                    label = { Text(stringResource(R.string.providers_models) + " (comma separated)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = form.temperature,
                        onValueChange = { v -> onUpdate { it.copy(temperature = v) } },
                        label = { Text(stringResource(R.string.providers_temperature)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = form.maxTokens,
                        onValueChange = { v -> onUpdate { it.copy(maxTokens = v) } },
                        label = { Text(stringResource(R.string.providers_max_tokens)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.providers_enabled), modifier = Modifier.weight(1f))
                    Switch(
                        checked = form.enabled,
                        onCheckedChange = { v -> onUpdate { it.copy(enabled = v) } }
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onTest,
                        enabled = !isTesting,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isTesting) "Testing…" else stringResource(R.string.providers_test))
                    }
                    OutlinedButton(
                        onClick = onFetchModels,
                        enabled = !isFetchingModels,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (isFetchingModels) "Fetching…"
                            else stringResource(R.string.providers_fetch_models)
                        )
                    }
                }

                testResult?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (it.startsWith("Failed")) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
                info?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onSave) {
                        Text(stringResource(R.string.action_save))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private fun typeLabel(type: ProviderType): Int = when (type) {
    ProviderType.OPENAI -> R.string.provider_type_openai
    ProviderType.ANTHROPIC -> R.string.provider_type_anthropic
    ProviderType.GEMINI -> R.string.provider_type_gemini
    ProviderType.COMPAT -> R.string.provider_type_compat
}