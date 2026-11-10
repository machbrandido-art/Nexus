package com.nexus.app.providers

import app.cash.turbine.test
import com.nexus.app.providers.compat.OpenAiCompatibleAdapter
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class OpenAiAdapterTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder()
            .readTimeout(5, TimeUnit.SECONDS)
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun buildAdapter(): OpenAiCompatibleAdapter {
        val config = ProviderConfig(
            id = "test",
            name = "Test",
            type = ProviderType.COMPAT,
            baseUrl = server.url("/v1").toString().trimEnd('/'),
            apiKey = "sk-test",
            models = listOf("test-model")
        )
        return OpenAiCompatibleAdapter(client, config)
    }

    @Test
    fun `streaming yields token events and completes`() = runTest {
        val sse = buildString {
            append("data: {\"choices\":[{\"delta\":{\"content\":\"Hello\"}}]}\n\n")
            append("data: {\"choices\":[{\"delta\":{\"content\":\" world\"}}]}\n\n")
            append("data: {\"choices\":[{\"delta\":{},\"finish_reason\":\"stop\"}]}\n\n")
            append("data: [DONE]\n\n")
        }
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sse)
        )

        val request = ChatRequest(
            model = "test-model",
            messages = listOf(ChatMessage(ChatMessage.Role.USER, "hi"))
        )

        buildAdapter().streamChat(request).test {
            val first = awaitItem()
            assertTrue(first is ChatEvent.Token && first.text == "Hello")
            val second = awaitItem()
            assertTrue(second is ChatEvent.Token && second.text == " world")
            val done = awaitItem()
            assertTrue(done is ChatEvent.Done)
            awaitComplete()
        }

        val recorded = server.takeRequest()
        assertEquals("Bearer sk-test", recorded.getHeader("Authorization"))
        assertTrue(recorded.path?.endsWith("/chat/completions") == true)
    }

    @Test
    fun `reasoning deltas come through as reasoning events`() = runTest {
        val sse = buildString {
            append("data: {\"choices\":[{\"delta\":{\"reasoning_content\":\"thinking\"}}]}\n\n")
            append("data: {\"choices\":[{\"delta\":{\"content\":\"answer\"},\"finish_reason\":\"stop\"}]}\n\n")
            append("data: [DONE]\n\n")
        }
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sse)
        )

        val request = ChatRequest(
            model = "test-model",
            messages = listOf(ChatMessage(ChatMessage.Role.USER, "hi"))
        )

        buildAdapter().streamChat(request).test {
            val r = awaitItem()
            assertTrue(r is ChatEvent.Reasoning && r.text == "thinking")
            val t = awaitItem()
            assertTrue(t is ChatEvent.Token && t.text == "answer")
            awaitComplete()
        }
    }

    @Test
    fun `401 produces unauthorized error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{\"error\":\"bad key\"}"))

        val request = ChatRequest(
            model = "test-model",
            messages = listOf(ChatMessage(ChatMessage.Role.USER, "hi"))
        )

        buildAdapter().streamChat(request).test {
            val item = awaitItem()
            assertTrue(item is ChatEvent.Error)
            val msg = (item as ChatEvent.Error).message
            assertTrue(msg.contains("Unauthorized", ignoreCase = true))
            awaitComplete()
        }
    }
}