package com.nexus.app.providers

import kotlinx.coroutines.flow.Flow

interface ProviderAdapter {
    suspend fun listModels(): List<ModelInfo>
    fun streamChat(request: ChatRequest): Flow<ChatEvent>
    suspend fun testConnection(): Result<Unit>
    val capabilities: Capabilities
}