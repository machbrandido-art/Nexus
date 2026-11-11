package com.nexus.app.providers

import app.cash.turbine.test
import com.nexus.app.providers.compat.NimPreset
import com.nexus.app.providers.compat.OpenAiCompatibleAdapter
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class NimPresetTest {

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

    private fun adapter(apiKey: String = "nvapi-test"): OpenAiCompatibleAdapter {
        val config = ProviderConfig(
            id = "nim-test",
            name = "NIM",
            type = ProviderType.COMPAT,
            baseUrl = server.url("/v1").toString().trimEnd('/'),
            apiKey = apiKey,
            presetId = NimPreset.ID
        )
        return OpenAiCompatibleAdapter(client, config)
    }

    @Test
    fun `model list parses vendor prefixed ids`() = runTest {
        val body = """
            {"data": [
              {"id": "meta/llama-3.1-70b-instruct"},
              {"id": "deepseek-ai/deepseek-r1"},
              {"id": "nvidia/nemotron-4-340b-instruct"}
            ]}
        """.trimIndent()
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(body)
        )

        val models = adapter().listModels()

        assertEquals(3, models.size)
        assertTrue(models.any { it.id == "meta/llama-3.1-70b-instruct" })
        assertTrue(models.any { it.id == "deepseek-ai/deepseek-r1" })
        assertTrue(models.any { it.id == "nvidia/nemotron-4-340b-instruct" })

        val recorded = server.takeRequest()
        assertEquals("Bearer nvapi-test", recorded.getHeader("Authorization"))
        assertTrue(recorded.path?.endsWith("/v1/models") == true)
    }

    @Test
    fun `streaming yields reasoning then tokens`() = runTest {
        val sse = buildString {
            append("data: {\"choices\":[{\"delta\":{\"reasoning_content\":\"thinking...\"}}]}\n\n")
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
            model = "meta/llama-3.1-70b-instruct",
            messages = listOf(ChatMessage(ChatMessage.Role.USER, "hi"))
        )

        adapter().streamChat(request).test {
            val r = awaitItem()
            assertTrue(r is ChatEvent.Reasoning && r.text == "thinking...")
            val t1 = awaitItem()
            assertTrue(t1 is ChatEvent.Token && t1.text == "Hello")
            val t2 = awaitItem()
            assertTrue(t2 is ChatEvent.Token && t2.text == " world")
            val done = awaitItem()
            assertTrue(done is ChatEvent.Done)
            awaitComplete()
        }
    }

    @Test
    fun `local endpoint sends no Authorization header when key blank`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"data":[{"id":"meta/llama-3.1-8b-instruct"}]}""")
        )

        val models = adapter(apiKey = "").listModels()

        assertEquals(1, models.size)
        assertEquals("meta/llama-3.1-8b-instruct", models.first().id)

        val recorded = server.takeRequest()
        assertNull(recorded.getHeader("Authorization"))
    }

    @Test
    fun `groupModelsByVendor groups by first segment`() {
        val groups = NimPreset.groupModelsByVendor(
            listOf(
                "meta/llama-3.1-70b",
                "meta/llama-3.1-8b",
                "deepseek-ai/deepseek-r1",
                "nvidia/nemotron",
                "no-slash-model"
            )
        )
        assertEquals(4, groups.size)
        assertEquals(2, groups["meta"]?.size)
        assertEquals(1, groups["deepseek-ai"]?.size)
        assertEquals(1, groups["nvidia"]?.size)
        assertEquals(1, groups["other"]?.size)
        // "other" is sorted last
        assertEquals("other", groups.keys.last())
    }

    @Test
    fun `errorMessageForStatus maps NIM status codes`() {
        assertEquals("Invalid NVIDIA key", NimPreset.errorMessageForStatus(401))
        assertEquals("Invalid NVIDIA key", NimPreset.errorMessageForStatus(403))
        assertTrue(
            NimPreset.errorMessageForStatus(429)
                ?.contains("Rate limit reached", ignoreCase = true) == true
        )
        assertEquals(
            "Model not available on this endpoint",
            NimPreset.errorMessageForStatus(404)
        )
        assertNull(NimPreset.errorMessageForStatus(500))
        assertNull(NimPreset.errorMessageForStatus(200))
    }

    @Test
    fun `isCloudEndpoint detects hosted vs self hosted`() {
        assertTrue(NimPreset.isCloudEndpoint("https://integrate.api.nvidia.com/v1"))
        assertTrue(NimPreset.isCloudEndpoint("https://integrate.api.nvidia.com/v1/"))
        assertTrue(!NimPreset.isCloudEndpoint("http://localhost:8000/v1"))
        assertTrue(!NimPreset.isCloudEndpoint("http://192.168.1.10:8000/v1"))
    }
}