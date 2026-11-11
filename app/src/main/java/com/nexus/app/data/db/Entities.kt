package com.nexus.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "providers", indices = [Index("type")])
data class ProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val baseUrl: String,
    @ColumnInfo(name = "api_key_encrypted") val apiKeyEncrypted: String,
    @ColumnInfo(name = "models_csv") val modelsCsv: String,
    val enabled: Boolean = true,
    val temperature: Float? = null,
    @ColumnInfo(name = "max_tokens") val maxTokens: Int? = null,
    @ColumnInfo(name = "reasoning_effort") val reasoningEffort: String? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    /** Null for hand-created providers, the preset id (e.g. "nim") for one-tap adds. */
    @ColumnInfo(name = "preset_id") val presetId: String? = null
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    @ColumnInfo(name = "provider_id") val providerId: String?,
    val model: String?,
    val mode: String = "chat",
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    val pinned: Boolean = false,
    val archived: Boolean = false
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversation_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversation_id")]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "conversation_id") val conversationId: String,
    val role: String,
    val content: String,
    @ColumnInfo(name = "reasoning") val reasoning: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(tableName = "kanban_cards")
data class KanbanCardEntity(
    @PrimaryKey val id: String,
    val title: String,
    val notes: String = "",
    val status: String = "TODO",
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "conversation_id") val conversationId: String? = null,
    @ColumnInfo(name = "due_at") val dueAt: Long? = null,
    @ColumnInfo(name = "labels_csv") val labelsCsv: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long
)