package com.nexus.app.features.kanban

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.app.R
import com.nexus.app.data.db.KanbanCardEntity
import com.nexus.app.ui.components.CircleIconButton
import com.nexus.app.ui.components.EmptyState
import com.nexus.app.ui.components.NexusButton
import com.nexus.app.ui.components.NexusButtonVariant
import com.nexus.app.ui.components.NexusTextField
import com.nexus.app.ui.components.PillTabs
import com.nexus.app.ui.theme.LocalNexusColors
import com.nexus.app.ui.theme.LocalNexusSpacing
import com.nexus.app.ui.theme.LocalNexusTypography
import com.nexus.app.ui.theme.NotchCorner
import com.nexus.app.ui.theme.NotchedRoundedShape
import org.koin.androidx.compose.koinViewModel

private data class KanbanStatus(val key: String, val labelRes: Int)

private val Statuses = listOf(
    KanbanStatus("TODO", R.string.kanban_todo),
    KanbanStatus("IN_PROGRESS", R.string.kanban_in_progress),
    KanbanStatus("REVIEW", R.string.kanban_review),
    KanbanStatus("DONE", R.string.kanban_done)
)

@Composable
fun KanbanScreen(viewModel: KanbanViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val c = LocalNexusColors.current
    val type = LocalNexusTypography.current
    val spacing = LocalNexusSpacing.current

    var tab by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var addForStatus by remember { mutableStateOf("TODO") }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragDx by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Spacer(Modifier.height(spacing.sm))
        BasicText(
            text = stringResource(R.string.kanban_title),
            style = type.display.copy(color = c.text),
            modifier = Modifier.padding(horizontal = spacing.md)
        )
        Spacer(Modifier.height(spacing.sm))
        PillTabs(
            items = listOf("Board", "Timeline"),
            selectedIndex = tab,
            onSelect = { tab = it }
        )
        Spacer(Modifier.height(spacing.sm))

        if (tab == 0) {
            val scroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scroll)
                    .padding(horizontal = spacing.md),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Statuses.forEachIndexed { columnIndex, status ->
                    val cards = state.cards.filter { it.status == status.key }
                        .sortedBy { it.sortOrder }
                    Column(
                        modifier = Modifier
                            .width(280.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(c.surfaceAlt)
                            .padding(spacing.sm)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BasicText(
                                text = stringResource(status.labelRes),
                                style = type.subtitle.copy(color = c.text),
                                modifier = Modifier.weight(1f)
                            )
                            BasicText(
                                text = cards.size.toString(),
                                style = type.caption.copy(color = c.textMuted)
                            )
                            Spacer(Modifier.width(6.dp))
                            CircleIconButton(
                                icon = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.kanban_add_card),
                                onClick = { addForStatus = status.key; showAddDialog = true },
                                variant = NexusButtonVariant.Ink,
                                size = 32.dp
                            )
                        }
                        Spacer(Modifier.height(spacing.sm))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                            itemsIndexed(cards, key = { _, c -> c.id }) { index, card ->
                                val isInk = index % 2 == 0
                                KanbanCard(
                                    card = card,
                                    isInk = isInk,
                                    isDragging = draggingId == card.id,
                                    modifier = Modifier.pointerInput(card.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { draggingId = card.id; dragDx = 0f },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                dragDx += amount.x
                                            },
                                            onDragEnd = {
                                                val dir = when {
                                                    dragDx > 160f -> 1
                                                    dragDx < -160f -> -1
                                                    else -> 0
                                                }
                                                if (dir != 0) {
                                                    val target = (columnIndex + dir)
                                                        .coerceIn(0, Statuses.lastIndex)
                                                    viewModel.moveCard(card.id, Statuses[target].key)
                                                }
                                                draggingId = null
                                                dragDx = 0f
                                            },
                                            onDragCancel = { draggingId = null; dragDx = 0f }
                                        )
                                    },
                                    onDelete = { viewModel.deleteCard(card.id) }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Timeline",
                    subtitle = "Coming soon — Gantt-style horizontal bars will land in a future update."
                )
            }
        }
    }

    if (showAddDialog) {
        AddCardDialog(
            onConfirm = { title, notes ->
                viewModel.addCard(title = title, notes = notes, status = addForStatus)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun KanbanCard(
    card: KanbanCardEntity,
    isInk: Boolean,
    isDragging: Boolean,
    modifier: Modifier = Modifier,
    onDelete: () -> Unit
) {
    val c = LocalNexusColors.current
    val type = LocalNexusTypography.current
    val spacing = LocalNexusSpacing.current
    val bg = if (isInk) c.ink else c.lime
    val fg = if (isInk) c.onInk else c.ink
    val shape = NotchedRoundedShape(corner = 24.dp, notchRadius = 20.dp, notchInset = 24.dp, notchCorner = NotchCorner.TopEnd)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isDragging) Modifier.padding(2.dp) else Modifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(bg)
                .padding(spacing.sm)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                BasicText(
                    text = card.title,
                    style = type.subtitle.copy(color = fg),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(fg.copy(alpha = 0.2f))
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = fg,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            if (card.notes.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                BasicText(
                    text = card.notes,
                    style = type.caption.copy(color = fg.copy(alpha = 0.8f)),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(spacing.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isInk) c.lime else c.ink),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = if (isInk) c.ink else c.onInk,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddCardDialog(
    onConfirm: (title: String, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalNexusColors.current
    val type = LocalNexusTypography.current
    val spacing = LocalNexusSpacing.current
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(c.bg)
                .padding(spacing.lg)
        ) {
            BasicText(
                text = stringResource(R.string.kanban_add_card),
                style = type.title.copy(color = c.text)
            )
            Spacer(Modifier.height(spacing.md))
            NexusTextField(
                value = title,
                onValueChange = { title = it },
                label = stringResource(R.string.kanban_card_title),
                singleLine = true
            )
            Spacer(Modifier.height(spacing.sm))
            NexusTextField(
                value = notes,
                onValueChange = { notes = it },
                label = stringResource(R.string.kanban_card_notes),
                maxLines = 3
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
                    onClick = { if (title.isNotBlank()) onConfirm(title.trim(), notes.trim()) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}