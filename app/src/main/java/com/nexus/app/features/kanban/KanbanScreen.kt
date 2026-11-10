package com.nexus.app.features.kanban

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.app.R
import com.nexus.app.data.db.KanbanCardEntity
import org.koin.androidx.compose.koinViewModel

private val Statuses = listOf("TODO" to R.string.kanban_todo, "IN_PROGRESS" to R.string.kanban_in_progress, "REVIEW" to R.string.kanban_review, "DONE" to R.string.kanban_done)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanScreen(viewModel: KanbanViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var addForStatus by remember { mutableStateOf("TODO") }

    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragDx by remember { mutableFloatStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.kanban_title)) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { addForStatus = "TODO"; showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.kanban_add_card))
            }
        }
    ) { padding ->
        val scroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .horizontalScroll(scroll)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Statuses.forEachIndexed { columnIndex, (statusKey, labelRes) ->
                val cards = state.cards.filter { it.status == statusKey }.sortedBy { it.sortOrder }
                Column(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = cards.size.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(onClick = {
                            addForStatus = statusKey
                            showAddDialog = true
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.kanban_add_card))
                        }
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items = cards, key = { it.id }) { card ->
                            KanbanCard(
                                card = card,
                                isDragging = draggingId == card.id,
                                modifier = Modifier.pointerInput(card.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingId = card.id
                                            dragDx = 0f
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragDx += amount.x
                                        },
                                        onDragEnd = {
                                            val direction = when {
                                                dragDx > 160f -> 1
                                                dragDx < -160f -> -1
                                                else -> 0
                                            }
                                            if (direction != 0) {
                                                val target = (columnIndex + direction)
                                                    .coerceIn(0, Statuses.lastIndex)
                                                viewModel.moveCard(card.id, Statuses[target].first)
                                            }
                                            draggingId = null
                                            dragDx = 0f
                                        },
                                        onDragCancel = {
                                            draggingId = null
                                            dragDx = 0f
                                        }
                                    )
                                },
                                onDelete = { viewModel.deleteCard(card.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.kanban_add_card)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.kanban_card_title)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(stringResource(R.string.kanban_card_notes)) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) {
                        viewModel.addCard(title = title.trim(), notes = notes.trim(), status = addForStatus)
                    }
                    showAddDialog = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun KanbanCard(
    card: KanbanCardEntity,
    isDragging: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isDragging) Modifier.padding(2.dp) else Modifier),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.Top) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.action_delete)
                    )
                }
            }
            if (card.notes.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = card.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}