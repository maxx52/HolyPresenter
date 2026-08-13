package holypresenter.org.platform.ai

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import holypresenter.org.platform.api.ai.AiGenerationRequest
import holypresenter.org.platform.api.ai.AiGenerationResult
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import java.net.InetSocketAddress
import java.net.URI
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OllamaProviderTest {
    private lateinit var server: HttpServer
    private lateinit var provider: OllamaProvider
    private val chatRequestBody = AtomicReference("")

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/tags") { exchange ->
            exchange.respond(200, """{"models":[{"model":"qwen3:1.7b"}]}""")
        }
        server.createContext("/api/chat") { exchange ->
            chatRequestBody.set(exchange.requestBody.bufferedReader().readText())
            exchange.respond(
                200,
                """{"message":{"role":"assistant","content":"Готовый локальный текст"},"prompt_eval_count":12,"eval_count":7}"""
            )
        }
        server.createContext("/api/pull") { exchange ->
            exchange.respond(
                200,
                """{"status":"pulling","total":100,"completed":50}
                   |{"status":"success"}
                """.trimMargin()
            )
        }
        server.start()
        provider = OllamaProvider(
            apiRoot = URI.create("http://127.0.0.1:${server.address.port}/api")
        )
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun status_detectsInstalledModel() = runBlocking {
        assertEquals(OllamaStatus.Ready, provider.status())
    }

    @Test
    fun generate_usesLocalModelWithoutApiKey() = runBlocking {
        val result = provider.generate(
            AiGenerationRequest.Text("Создай объявление", "ignored-cloud-model", 600)
        )

        val text = assertIs<AiGenerationResult.Text>(result)
        assertEquals("Готовый локальный текст", text.text)
        assertEquals(0.0, text.costUsd)
        assertEquals(12, text.usage.inputTokens)
        assertEquals(7, text.usage.outputTokens)
        assertTrue(chatRequestBody.get().contains("qwen3:1.7b"))
        assertTrue(chatRequestBody.get().contains("\"think\":false"))
    }

    @Test
    fun installModel_reportsProgressAndRemainsFree() = runBlocking {
        val progress = mutableListOf<Int>()

        val status = provider.installModel { percent, _ -> progress += percent }

        assertEquals(OllamaStatus.Ready, status)
        assertTrue(50 in progress)
        assertEquals(100, progress.last())
    }

    private fun HttpExchange.respond(status: Int, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        responseHeaders.add("Content-Type", "application/json; charset=utf-8")
        sendResponseHeaders(status, bytes.size.toLong())
        responseBody.use { it.write(bytes) }
    }
}
