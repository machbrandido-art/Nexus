package com.nexus.app.data

import com.nexus.app.data.db.ConversationDao
import com.nexus.app.data.db.ConversationEntity
import com.nexus.app.data.db.MessageDao
import com.nexus.app.data.db.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class ConversationRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {

    fun observeAll(): Flow<List<ConversationEntity>> = conversationDao.observeAll()

    fun observeConversation(id: String): Flow<ConversationEntity?> = conversationDao.observeById(id)

    fun observeMessages(conversationId: String): Flow<List<MessageEntity>> =
        messageDao.observeForConversation(conversationId)

    suspend fun createConversation(
        title: String = "New chat",
        providerId: String? = null,
        model: String? = null,
        mode: String = "chat"
    ): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        conversationDao.upsert(
            ConversationEntity(
                id = id,
                title = title,
                providerId = providerId,
                model = model,
                mode = mode,
                updatedAt = now
            )
        )
        return id
    }

    suspend fun ensureConversation(id: String?): String {
        if (id != null) {
            val existing = conversationDao.observeById(id).first()
            if (existing != null) return id
        }
        return createConversation()
    }

    suspend fun appendMessage(
        conversationId: String,
        role: String,
        content: String,
        reasoning: String? = null,
        touchTitle: String? = null
    ): String {
        val id = UUID.randomUUID().toString()
        messageDao.upsert(
            MessageEntity(
                id = id,
                conversationId = conversationId,
                role = role,
                content = content,
                reasoning = reasoning,
                createdAt = System.currentTimeMillis()
            )
        )
        val title = touchTitle ?: conversationDao.observeById(conversationId).first()?.title ?: "New chat"
        conversationDao.touch(conversationId, System.currentTimeMillis(), title)
        return id
    }

    suspend fun updateConversationMeta(
        id: String,
        providerId: String?,
        model: String?,
        mode: String
    ) {
        val existing = conversationDao.observeById(id).first() ?: return
        conversationDao.upsert(
            existing.copy(providerId = providerId, model = model, mode = mode, updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun renameConversation(id: String, newTitle: String) {
        conversationDao.touch(id, System.currentTimeMillis(), newTitle)
    }

    suspend fun setPinned(id: String, pinned: Boolean) {
        val existing = conversationDao.observeById(id).first() ?: return
        conversationDao.upsert(existing.copy(pinned = pinned))
    }

    suspend fun setArchived(id: String, archived: Boolean) {
        val existing = conversationDao.observeById(id).first() ?: return
        conversationDao.upsert(existing.copy(archived = archived))
    }

    suspend fun deleteConversation(id: String) {
        messageDao.deleteForConversation(id)
        conversationDao.deleteById(id)
    }
}