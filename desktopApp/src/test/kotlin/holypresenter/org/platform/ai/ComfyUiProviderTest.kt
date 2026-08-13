package holypresenter.org.platform.ai

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import holypresenter.org.platform.api.ai.AiGenerationRequest
import holypresenter.org.platform.api.ai.AiImageQuality
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.URI
import java.util.concurrent.atomic.AtomicReference
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ComfyUiProviderTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var server: HttpServer
    private lateinit var provider: ComfyUiProvider
    private val submittedWorkflow = AtomicReference("")

    @Before
    fun setUp() {
        val imageBytes = ByteArrayOutputStream().use { output ->
            val image = BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB)
            val graphics = image.createGraphics()
            graphics.color = Color(30, 80, 160)
            graphics.fillRect(0, 0, image.width, image.height)
            graphics.dispose()
            ImageIO.write(image, "png", output)
            output.toByteArray()
        }
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/models/checkpoints") { exchange ->
            exchange.respond(200, "[\"sd15.safetensors\"]")
        }
        server.createContext("/prompt") { exchange ->
            submittedWorkflow.set(exchange.requestBody.bufferedReader().readText())
            exchange.respond(200, "{\"prompt_id\":\"holy-test\",\"number\":1}")
        }
        server.createContext("/history/holy-test") { exchange ->
            exchange.respond(
                200,
                """{"holy-test":{"status":{"status_str":"success"},"outputs":{"9":{"images":[{"filename":"result.png","subfolder":"","type":"output"}]}}}}"""
            )
        }
        server.createContext("/view") { exchange ->
            exchange.respond(200, imageBytes, "image/png")
        }
        server.start()
        provider = ComfyUiProvider(
            applicationHome = temporaryFolder.root,
            serverRoot = URI.create("http://127.0.0.1:${server.address.port}")
        )
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun status_detectsCheckpoint() = runBlocking {
        val status = assertIs<ComfyUiStatus.Ready>(provider.status())
        assertEquals("sd15.safetensors", status.checkpoint)
    }

    @Test
    fun generate_submitsOfficialWorkflowAndSavesFreeImage() = runBlocking {
        val result = provider.generate(
            AiGenerationRequest.Image(
                prompt = "Спокойный фон для библейского стиха",
                model = "unused",
                quality = AiImageQuality.MEDIUM,
                size = "1536x1024"
            )
        )

        assertEquals(0.0, result.costUsd)
        assertTrue(java.io.File(result.filePath).isFile)
        assertTrue(submittedWorkflow.get().contains("CheckpointLoaderSimple"))
        assertTrue(submittedWorkflow.get().contains("sd15.safetensors"))
        assertTrue(submittedWorkflow.get().contains("SaveImage"))
    }

    private fun HttpExchange.respond(
        status: Int,
        body: String
    ) = respond(status, body.toByteArray(Charsets.UTF_8), "application/json; charset=utf-8")

    private fun HttpExchange.respond(
        status: Int,
        body: ByteArray,
        contentType: String
    ) {
        responseHeaders.add("Content-Type", contentType)
        sendResponseHeaders(status, body.size.toLong())
        responseBody.use { it.write(body) }
    }
}
