package holypresenter.org.platform.ai

import holypresenter.org.platform.api.ai.AiCostEstimate
import holypresenter.org.platform.api.ai.AiGenerationKind
import holypresenter.org.platform.api.ai.AiGenerationRequest
import holypresenter.org.platform.api.ai.AiGenerationResult
import holypresenter.org.platform.api.ai.AiProvider
import holypresenter.org.platform.api.ai.AiTokenUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.net.ConnectException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.random.Random

sealed interface ComfyUiStatus {
    data object Checking : ComfyUiStatus
    data object NotRunning : ComfyUiStatus
    data object ModelMissing : ComfyUiStatus
    data class Ready(val checkpoint: String) : ComfyUiStatus
    data class Failed(val message: String) : ComfyUiStatus
}

class ComfyUiProvider(
    applicationHome: File,
    private val serverRoot: URI = URI.create("http://127.0.0.1:8188"),
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build()
) : AiProvider {
    override val id: String = "comfyui-local"
    override val displayName: String = "Локальная нейросеть ComfyUI"
    override val supportedKinds: Set<AiGenerationKind> = setOf(AiGenerationKind.IMAGE)

    private val generatedDirectory = File(applicationHome, "ai/generated")
    private val json = Json { ignoreUnknownKeys = true }

    override fun estimate(request: AiGenerationRequest): AiCostEstimate {
        require(request is AiGenerationRequest.Image) {
            "ComfyUI поддерживает только изображения"
        }
        return AiCostEstimate(0.0, "бесплатно · локально · требуется видеокарта")
    }

    suspend fun status(): ComfyUiStatus = withContext(Dispatchers.IO) {
        runCatching {
            val checkpoints = checkpoints()
            if (checkpoints.isEmpty()) ComfyUiStatus.ModelMissing
            else ComfyUiStatus.Ready(checkpoints.first())
        }.getOrElse { throwable ->
            if (throwable.isConnectionFailure()) ComfyUiStatus.NotRunning
            else ComfyUiStatus.Failed(throwable.message ?: "Не удалось проверить ComfyUI")
        }
    }

    override suspend fun generate(
        request: AiGenerationRequest,
        onProgress: (percent: Int, message: String) -> Unit
    ): AiGenerationResult.Media = withContext(Dispatchers.IO) {
        require(request is AiGenerationRequest.Image) {
            "ComfyUI поддерживает только изображения"
        }
        require(request.prompt.isNotBlank()) { "Опишите изображение" }
        onProgress(3, "Проверяем ComfyUI и модель…")
        val checkpoint = checkpoints().firstOrNull()
            ?: error("В ComfyUI нет модели checkpoint. Установите модель и нажмите «Проверить снова».")
        val (width, height) = generationSize(request.size)
        val promptId = submitWorkflow(
            workflow = workflow(
                checkpoint = checkpoint,
                prompt = request.prompt,
                width = width,
                height = height
            )
        )
        onProgress(12, "Изображение поставлено в очередь…")
        val image = waitForImage(promptId, onProgress)
        onProgress(92, "Сохраняем изображение…")
        val bytes = downloadImage(image)
        val destination = generatedFile("comfyui-image", image.extension())
        writeAtomically(destination, bytes)
        onProgress(100, "Локальное изображение готово")
        AiGenerationResult.Media(
            providerId = id,
            filePath = destination.absolutePath,
            mimeType = image.mimeType(),
            kind = AiGenerationKind.IMAGE,
            costUsd = 0.0,
            usage = AiTokenUsage()
        )
    }

    private fun checkpoints(): List<String> {
        val response = sendGet("/models/checkpoints", Duration.ofSeconds(5))
        return json.parseToJsonElement(response).jsonArray
            .mapNotNull { it.jsonPrimitive.contentOrNull }
            .filter(String::isNotBlank)
    }

    private fun submitWorkflow(workflow: JsonObject): String {
        val body = buildJsonObject {
            put("prompt", workflow)
            put("client_id", UUID.randomUUID().toString())
        }
        val request = HttpRequest.newBuilder(endpoint("/prompt"))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString(), Charsets.UTF_8))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(Charsets.UTF_8))
        if (response.statusCode() !in 200..299) {
            error(comfyError(response.statusCode(), response.body()))
        }
        val root = json.parseToJsonElement(response.body()).jsonObject
        return root["prompt_id"]?.jsonPrimitive?.contentOrNull
            ?: error(root["error"]?.jsonPrimitive?.contentOrNull ?: "ComfyUI не вернула номер задания")
    }

    private suspend fun waitForImage(
        promptId: String,
        onProgress: (percent: Int, message: String) -> Unit
    ): ComfyImage {
        val deadline = System.nanoTime() + Duration.ofMinutes(15).toNanos()
        var attempt = 0
        while (System.nanoTime() < deadline) {
            delay(1_000)
            attempt++
            val body = sendGet("/history/$promptId", Duration.ofSeconds(10))
            val root = json.parseToJsonElement(body).jsonObject
            val history = root[promptId]?.jsonObject ?: continue
            val status = history["status"]?.jsonObject
            if (status?.get("status_str")?.jsonPrimitive?.contentOrNull == "error") {
                error("ComfyUI не смогла создать изображение. Проверьте модель и журнал ComfyUI.")
            }
            val images = history["outputs"]?.jsonObject
                ?.get(SAVE_NODE_ID)?.jsonObject
                ?.get("images")?.jsonArray
            val first = images?.firstOrNull()?.jsonObject
            if (first != null) {
                return ComfyImage(
                    filename = first["filename"]?.jsonPrimitive?.contentOrNull
                        ?: error("ComfyUI не указала имя изображения"),
                    subfolder = first["subfolder"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    type = first["type"]?.jsonPrimitive?.contentOrNull ?: "output"
                )
            }
            val percent = (12 + attempt / 2).coerceAtMost(88)
            onProgress(percent, "ComfyUI создаёт изображение…")
        }
        error("ComfyUI не завершила изображение за 15 минут")
    }

    private fun downloadImage(image: ComfyImage): ByteArray {
        val query = "filename=${image.filename.encoded()}&subfolder=${image.subfolder.encoded()}&type=${image.type.encoded()}"
        val request = HttpRequest.newBuilder(endpoint("/view?$query"))
            .timeout(Duration.ofMinutes(2))
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
        if (response.statusCode() !in 200..299) {
            error("ComfyUI не отдала готовое изображение (${response.statusCode()})")
        }
        check(response.body().isNotEmpty()) { "ComfyUI вернула пустой файл" }
        return response.body()
    }

    private fun workflow(
        checkpoint: String,
        prompt: String,
        width: Int,
        height: Int
    ): JsonObject = buildJsonObject {
        put("3", node("KSampler", buildJsonObject {
            put("seed", Random.nextLong(0, Long.MAX_VALUE))
            put("steps", 20)
            put("cfg", 7.0)
            put("sampler_name", "euler")
            put("scheduler", "normal")
            put("denoise", 1.0)
            put("model", reference("4", 0))
            put("positive", reference("6", 0))
            put("negative", reference("7", 0))
            put("latent_image", reference("5", 0))
        }))
        put("4", node("CheckpointLoaderSimple", buildJsonObject {
            put("ckpt_name", checkpoint)
        }))
        put("5", node("EmptyLatentImage", buildJsonObject {
            put("width", width)
            put("height", height)
            put("batch_size", 1)
        }))
        put("6", node("CLIPTextEncode", buildJsonObject {
            put("text", "$prompt, high quality, cinematic church presentation background, no text, 16:9")
            put("clip", reference("4", 1))
        }))
        put("7", node("CLIPTextEncode", buildJsonObject {
            put("text", "text, letters, words, watermark, logo, blurry, distorted, low quality")
            put("clip", reference("4", 1))
        }))
        put("8", node("VAEDecode", buildJsonObject {
            put("samples", reference("3", 0))
            put("vae", reference("4", 2))
        }))
        put(SAVE_NODE_ID, node("SaveImage", buildJsonObject {
            put("filename_prefix", "HolyPresenter")
            put("images", reference("8", 0))
        }))
    }

    private fun node(classType: String, inputs: JsonObject): JsonObject = buildJsonObject {
        put("class_type", classType)
        put("inputs", inputs)
    }

    private fun reference(nodeId: String, output: Int): JsonArray = buildJsonArray {
        add(JsonPrimitive(nodeId))
        add(JsonPrimitive(output))
    }

    private fun generationSize(requested: String): Pair<Int, Int> {
        val parts = requested.lowercase().split('x')
        val width = parts.getOrNull(0)?.toIntOrNull() ?: 1_536
        val height = parts.getOrNull(1)?.toIntOrNull() ?: 1_024
        return when {
            width == height -> 512 to 512
            width > height -> 768 to 512
            else -> 512 to 768
        }
    }

    private fun sendGet(path: String, timeout: Duration): String {
        val request = HttpRequest.newBuilder(endpoint(path))
            .timeout(timeout)
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(Charsets.UTF_8))
        if (response.statusCode() !in 200..299) {
            error(comfyError(response.statusCode(), response.body()))
        }
        return response.body()
    }

    private fun comfyError(status: Int, body: String): String {
        val message = runCatching {
            val root = json.parseToJsonElement(body).jsonObject
            root["error"]?.let { value ->
                if (value is JsonObject) {
                    value["message"]?.jsonPrimitive?.contentOrNull
                } else {
                    value.jsonPrimitive.contentOrNull
                }
            }
        }.getOrNull()
        return if (message.isNullOrBlank()) "Ошибка ComfyUI ($status)" else "Ошибка ComfyUI: ${message.take(500)}"
    }

    private fun endpoint(path: String): URI = serverRoot.resolve(path)

    private fun String.encoded(): String = URLEncoder.encode(this, Charsets.UTF_8)

    private fun Throwable.isConnectionFailure(): Boolean =
        this is ConnectException || cause is ConnectException ||
            javaClass.simpleName.contains("ConnectTimeout", ignoreCase = true)

    private fun generatedFile(prefix: String, extension: String): File {
        generatedDirectory.mkdirs()
        val timestamp = FILE_TIME_FORMAT.format(LocalDateTime.now())
        return File(generatedDirectory, "$prefix-$timestamp-${UUID.randomUUID().toString().take(8)}.$extension")
    }

    private fun writeAtomically(destination: File, bytes: ByteArray) {
        destination.parentFile?.mkdirs()
        val temporary = File(destination.parentFile, ".${destination.name}.tmp")
        temporary.writeBytes(bytes)
        runCatching {
            Files.move(
                temporary.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        }.getOrElse {
            Files.move(
                temporary.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    private data class ComfyImage(
        val filename: String,
        val subfolder: String,
        val type: String
    ) {
        fun extension(): String = filename.substringAfterLast('.', "png")
            .lowercase()
            .takeIf { it in setOf("png", "jpg", "jpeg", "webp") }
            ?: "png"

        fun mimeType(): String = when (extension()) {
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            else -> "image/png"
        }
    }

    companion object {
        const val DOWNLOAD_URL: String = "https://www.comfy.org/download"
        const val INSTALL_GUIDE_URL: String = "https://docs.comfy.org/get_started/first_generation"
        private const val SAVE_NODE_ID: String = "9"
        private val FILE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    }
}
