package com.nexus.app.features.providers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.app.R
import com.nexus.app.providers.ModelInfo
import com.nexus.app.providers.ProviderConfig
import com.nexus.app.providers.ProviderType
import com.nexus.app.providers.compat.AllProviderPresets
import com.nexus.app.providers.compat.NimPreset
import com.nexus.app.providers.compat.presetById
import com.nexus.app.ui.components.CircleIconButton
import com.nexus.app.ui.components.EmptyState
import com.nexus.app.ui.components.NexusButton
import com.nexus.app.ui.components.NexusButtonVariant
import com.nexus.app.ui.components.NexusMenu
import com.nexus.app.ui.components.NexusMenuItem
import com.nexus.app.ui.components.NexusSwitch
import com.nexus.app.ui.components.NexusTextField
import com.nexus.app.ui.components.NotchedCard
import com.nexus.app.ui.components.PillTabs
import com.nexus.app.ui.theme.LocalNexusColors
import com.nexus.app.ui.theme.LocalNexusSpacing
import com.nexus.app.ui.theme.LocalNexusTypography
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProvidersScreen(viewModel: ProvidersViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val c = LocalNexusColors.current
    val type = LocalNexusTypography.current
    val spacing = LocalNexusSpacing.current

    var filterTab by remember { mutableStateOf(0) }
    var deleteTarget by remember { mutableStateOf<ProviderConfig?>(null) }

    val filters = listOf("All", "Cloud", "Local")
    val filtered = remember(state.providers, filterTab) {
        when (filterTab) {
            1 -> state.providers.filter { it.isCloudEndpoint() }
            2 -> state.providers.filter { !it.isCloudEndpoint() }
            else -> state.providers
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = spacing.md)
    ) {
        Spacer(Modifier.height(spacing.sm))
        BasicText(
            text = stringResource(R.string.providers_title),
            style = type.display.copy(color = c.text)
        )
        Spacer(Modifier.height(spacing.sm))
        PillTabs(
            items = filters,
            selectedIndex = filterTab,
            onSelect = { filterTab = it }
        )
        Spacer(Modifier.height(spacing.sm))

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EmptyState(
                        title = stringResource(R.string.providers_empty),
                        subtitle = stringResource(R.string.providers_quick_add_subtitle)
                    )
                    Spacer(Modifier.height(spacing.md))
                    NexusButton(
                        text = stringResource(R.string.providers_add),
                        onClick = viewModel::startAdd,
                        variant = NexusButtonVariant.Lime
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                itemsIndexed(filtered, key = { _, p -> p.id }) { index, provider ->
                    val isInk = index % 2 == 0
                    val bg = if (isInk) c.ink else c.lime
                    val fg = if (isInk) c.onInk else c.ink
                    val presetLabel = provider.presetId?.let { id ->
                        AllProviderPresets.firstOrNull { it.id == id }?.name
                    } ?: provider.type.name
                    val badgeText = if (provider.enabled) "Active" else "Off"

                    NotchedCard(
                        badgeText = badgeText,
                        badgeVariant = com.nexus.app.ui.components.BadgeVariant.Ink,
                        background = bg,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(spacing.md)
                    ) {
                        BasicText(
                            text = provider.name,
                            style = type.subtitle.copy(color = fg),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        BasicText(
                            text = "$presetLabel · ${provider.models.size} models",
                            style = type.caption.copy(color = fg.copy(alpha = 0.75f))
                        )
                        Spacer(Modifier.height(spacing.sm))
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                            IconPill(
                                icon = Icons.Filled.ContentCopy,
                                contentDescription = stringResource(R.string.action_duplicate),
                                tint = fg,
                                onClick = { viewModel.duplicate(provider) }
                            )
                            IconPill(
                                icon = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = fg,
                                onClick = { deleteTarget = provider }
                            )
                            Spacer(Modifier.weight(1f))
                            NexusButton(
                                text = stringResource(R.string.action_edit),
                                onClick = { viewModel.startEdit(provider) },
                                variant = if (isInk) NexusButtonVariant.Surface else NexusButtonVariant.Ink
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(spacing.sm))
        NexusButton(
            text = "+ " + stringResource(R.string.providers_add),
            onClick = viewModel::startAdd,
            variant = NexusButtonVariant.Ink,
            fullWidth = true
        )
        Spacer(Modifier.height(spacing.sm))
    }

    if (state.editing != null) {
        ProviderEditDialog(
            form = state.editing!!,
            isTesting = state.isTesting,
            isFetchingModels = state.isFetchingModels,
            testResult = state.testResult,
            info = state.info,
            error = state.error,
            canBrowseModels = state.fetchedModels.isNotEmpty(),
            onUpdate = viewModel::updateForm,
            onTypeChange = viewModel::onTypeChange,
            onTest = viewModel::testConnection,
            onFetchModels = viewModel::fetchModels,
            onBrowseModels = viewModel::openModelPicker,
            onSave = viewModel::save,
            onCancel = viewModel::cancelEdit
        )
    }

    if (state.showModelPicker) {
        NimModelPickerDialog(
            allModels = state.fetchedModels,
            selectedIds = state.selectedModelIds,
            onToggle = viewModel::toggleModel,
            onApply = viewModel::applyModelSelection,
            onDismiss = viewModel::closeModelPicker
        )
    }

    val target = deleteTarget
    if (target != null) {
        ConfirmDialog(
            title = stringResource(R.string.providers_delete_confirm),
            body = stringResource(R.string.providers_delete_confirm_body),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                viewModel.delete(target)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

private fun ProviderConfig.isCloudEndpoint(): Boolean =
    presetId != "ollama" && baseUrl.trim().startsWith("https://", ignoreCase = true)

@Composable
private fun IconPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.15f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(18.dp))
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
    canBrowseModels: Boolean,
    onUpdate: ((ProviderForm) -> ProviderForm) -> Unit,
    onTypeChange: (ProviderType) -> Unit,
    onTest: () -> Unit,
    onFetchModels: () -> Unit,
    onBrowseModels: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val c = LocalNexusColors.current
    val type = LocalNexusTypography.current
    val spacing = LocalNexusSpacing.current
    var typeMenuOpen by remember { mutableStateOf(false) }
    val preset = presetById(form.presetId)
    val keyRequired = preset?.apiKeyRequired ?: true
    val isNim = form.presetId == NimPreset.ID
    val hint = preset?.apiKeyHint

    val keyHint: String? = when {
        isNim && form.apiKey.isNotBlank() -> "Starts with nvapi-"
        isNim && !NimPreset.isCloudEndpoint(form.baseUrl) -> "Leave empty for local containers"
        hint != null -> "Starts with $hint"
        else -> null
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(c.bg)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(spacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = stringResource(
                        if (form.id == null) R.string.providers_add else R.string.providers_edit_title
                    ),
                    style = type.title.copy(color = c.text),
                    modifier = Modifier.weight(1f)
                )
                CircleIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.action_close),
                    onClick = onCancel,
                    variant = NexusButtonVariant.Surface
                )
            }
            Spacer(Modifier.height(spacing.md))

            NexusTextField(
                value = form.name,
                onValueChange = { v -> onUpdate { it.copy(name = v) } },
                label = stringResource(R.string.providers_name),
                singleLine = true
            )
            Spacer(Modifier.height(spacing.sm))

            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(c.surfaceAlt)
                        .clickable { typeMenuOpen = true }
                        .padding(horizontal = spacing.md, vertical = spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        text = stringResource(typeLabel(form.type)),
                        style = type.body.copy(color = c.text),
                        modifier = Modifier.weight(1f)
                    )
                    BasicText(
                        text = "▾",
                        style = type.body.copy(color = c.textMuted)
                    )
                }
                NexusMenu(
                    expanded = typeMenuOpen,
                    onDismissRequest = { typeMenuOpen = false },
                    alignment = Alignment.TopStart,
                    offset = IntOffset(0, 56)
                ) {
                    ProviderType.entries.forEach { t ->
                        NexusMenuItem(
                            text = stringResource(typeLabel(t)),
                            selected = t == form.type,
                            onClick = { onTypeChange(t); typeMenuOpen = false }
                        )
                    }
                }
            }
            Spacer(Modifier.height(spacing.sm))

            NexusTextField(
                value = form.baseUrl,
                onValueChange = { v -> onUpdate { it.copy(baseUrl = v) } },
                label = stringResource(R.string.providers_base_url),
                singleLine = true
            )
            if (isNim) {
                Spacer(Modifier.height(4.dp))
                BasicText(
                    text = "Cloud: ${NimPreset.CLOUD_BASE_URL}  ·  Local: ${NimPreset.SELF_HOSTED_HINT}",
                    style = type.caption.copy(color = c.textMuted),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            Spacer(Modifier.height(spacing.sm))

            NexusTextField(
                value = form.apiKey,
                onValueChange = { v -> onUpdate { it.copy(apiKey = v) } },
                label = stringResource(
                    if (keyRequired) R.string.providers_api_key else R.string.providers_api_key_optional
                ),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            if (keyHint != null) {
                Spacer(Modifier.height(4.dp))
                BasicText(
                    text = keyHint,
                    style = type.caption.copy(color = c.textMuted),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            Spacer(Modifier.height(spacing.sm))

            NexusTextField(
                value = form.modelsCsv,
                onValueChange = { v -> onUpdate { it.copy(modelsCsv = v) } },
                label = stringResource(R.string.providers_models) + " (comma separated)",
                maxLines = 4
            )
            Spacer(Modifier.height(spacing.sm))

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                NexusTextField(
                    value = form.temperature,
                    onValueChange = { v -> onUpdate { it.copy(temperature = v) } },
                    label = stringResource(R.string.providers_temperature),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                NexusTextField(
                    value = form.maxTokens,
                    onValueChange = { v -> onUpdate { it.copy(maxTokens = v) } },
                    label = stringResource(R.string.providers_max_tokens),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(spacing.sm))

            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = stringResource(R.string.providers_enabled),
                    style = type.body.copy(color = c.text),
                    modifier = Modifier.weight(1f)
                )
                NexusSwitch(
                    checked = form.enabled,
                    onCheckedChange = { v -> onUpdate { it.copy(enabled = v) } }
                )
            }
            Spacer(Modifier.height(spacing.md))

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                NexusButton(
                    text = if (isTesting) "Testing…" else stringResource(R.string.providers_test),
                    onClick = onTest,
                    enabled = !isTesting,
                    variant = NexusButtonVariant.Surface,
                    modifier = Modifier.weight(1f)
                )
                NexusButton(
                    text = if (isFetchingModels) "Fetching…"
                    else stringResource(R.string.providers_fetch_models),
                    onClick = onFetchModels,
                    enabled = !isFetchingModels,
                    variant = NexusButtonVariant.Surface,
                    modifier = Modifier.weight(1f)
                )
            }
            if (canBrowseModels) {
                Spacer(Modifier.height(spacing.xs))
                NexusButton(
                    text = "Browse models",
                    onClick = onBrowseModels,
                    variant = NexusButtonVariant.Lime,
                    fullWidth = true
                )
            }

            if (testResult != null) {
                Spacer(Modifier.height(spacing.sm))
                BasicText(
                    text = testResult,
                    style = type.caption.copy(
                        color = if (testResult.startsWith("Failed")) c.error else c.success
                    )
                )
            }
            if (info != null) {
                Spacer(Modifier.height(spacing.xs))
                BasicText(text = info, style = type.caption.copy(color = c.textMuted))
            }
            if (error != null) {
                Spacer(Modifier.height(spacing.xs))
                BasicText(text = error, style = type.caption.copy(color = c.error))
            }

            Spacer(Modifier.height(spacing.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                NexusButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onCancel,
                    variant = NexusButtonVariant.Surface,
                    modifier = Modifier.weight(1f)
                )
                NexusButton(
                    text = stringResource(R.string.action_save),
                    onClick = onSave,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(spacing.md))
        }
    }
}

@Composable
private fun NimModelPickerDialog(
    allModels: List<ModelInfo>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalNexusColors.current
    val type = LocalNexusTypography.current
    val spacing = LocalNexusSpacing.current
    var search by remember { mutableStateOf("") }
    var selectedVendor by remember { mutableStateOf<String?>(null) }

    val vendorKeys = NimPreset.groupModelsByVendor(allModels.map { it.id }).keys.toList()
    val vendors: List<String?> = listOf(null) + vendorKeys
    val filtered = allModels.filter { model ->
        val matchesSearch = search.isBlank() || model.id.contains(search, ignoreCase = true)
        val matchesVendor = selectedVendor == null || run {
            val slash = model.id.indexOf('/')
            val prefix = if (slash > 0) model.id.substring(0, slash) else "other"
            prefix == selectedVendor
        }
        matchesSearch && matchesVendor
    }

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
                    text = "Choose models (${selectedIds.size} selected)",
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
            Spacer(Modifier.height(spacing.xs))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                items(vendors) { vendor ->
                    val label = vendor ?: "All"
                    val selected = selectedVendor == vendor
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) c.lime else c.surface)
                            .clickable { selectedVendor = vendor }
                            .padding(horizontal = spacing.sm, vertical = 6.dp)
                    ) {
                        BasicText(
                            text = label,
                            style = type.caption.copy(color = if (selected) c.ink else c.textMuted)
                        )
                    }
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(filtered, key = { _, m -> m.id }) { _, model ->
                    val selected = model.id in selectedIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) c.limeSoft else c.surface)
                            .clickable { onToggle(model.id) }
                            .padding(horizontal = spacing.md, vertical = spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (selected) Icons.Filled.Check else Icons.Filled.Add,
                            contentDescription = null,
                            tint = if (selected) c.ink else c.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(spacing.sm))
                        BasicText(
                            text = model.id,
                            style = type.body.copy(color = c.text),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                NexusButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onDismiss,
                    variant = NexusButtonVariant.Surface,
                    modifier = Modifier.weight(1f)
                )
                NexusButton(
                    text = stringResource(R.string.action_apply),
                    onClick = onApply,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
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
                    text = confirmLabel,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                )
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