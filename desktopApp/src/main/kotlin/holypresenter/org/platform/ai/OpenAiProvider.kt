package holypresenter.org.platform.ai

import holypresenter.org.platform.api.ai.AiCostEstimate
import holypresenter.org.platform.api.ai.AiGenerationKind
import holypresenter.org.platform.api.ai.AiGenerationRequest
import holypresenter.org.platform.api.ai.AiGenerationResult
import holypresenter.org.platform.api.ai.AiImageQuality
import holypresenter.org.platform.api.ai.AiProvider
import holypresenter.org.platform.api.ai.AiTokenUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.UUID

class OpenAiProvider(
    private val apiKeyStore: OpenAiApiKeyStore,
    applicationHome: File,
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
) : AiProvider {
    override val id: String = "openai"
    override val displayName: String = "OpenAI"
    override val supportedKinds: Set<AiGenerationKind> = AiGenerationKind.entries.toSet()

    private val generatedDirectory = File(applicationHome, "ai/generated")
    private val json = Json { ignoreUnknownKeys = true }

    override fun estimate(request: AiGenerationRequest): AiCostEstimate = when (request) {
        is AiGenerationRequest.Text -> estimateText(request)
        is AiGenerationRequest.Image -> estimateImage(request)
        is AiGenerationRequest.Video -> estimateVideo(request)
    }

    override suspend fun generate(
        request: AiGenerationRequest,
        onProgress: (Int, String) -> Unit
    ): AiGenerationResult = withContext(Dispatchers.IO) {
        require(request.prompt.isNotBlank()) { "Введите запрос для ИИ-помощника" }
        when (request) {
            is AiGenerationRequest.Text -> generateText(request, onProgress)
            is AiGenerationRequest.Image -> generateImage(request, onProgress)
            is AiGenerationRequest.Video -> generateVideo(request, onProgress)
        }
    }

    private fun estimateText(request: AiGenerationRequest.Text): AiCostEstimate {
        val pricing = textPricing(request.model)
        // Deliberately conservative for Cyrillic and request framing.
        val estimatedInputTokens = request.prompt.length.toLong().coerceAtLeast(64L) + 500L
        val estimatedOutputTokens = request.maxOutputTokens.toLong()
        val usd = tokenCost(
            inputTokens = estimatedInputTokens,
            cachedInputTokens = 0,
            outputTokens = estimatedOutputTokens,
            pricing = pricing
        )
        return AiCostEstimate(
            usd = usd,
            description = "до ${request.maxOutputTokens} выходных токенов"
        )
    }

    private fun estimateImage(request: AiGenerationRequest.Image): AiCostEstimate {
        require(request.model == "gpt-image-2") {
            "Для модели ${request.model} не настроен безопасный расчёт стоимости"
        }
        val landscape = request.size != "1024x1024"
        val cost = when (request.quality) {
            AiImageQuality.LOW -> if (landscape) 0.005 else 0.006
            AiImageQuality.MEDIUM -> if (landscape) 0.041 else 0.053
            AiImageQuality.HIGH -> if (landscape) 0.165 else 0.211
        }
        return AiCostEstimate(cost, "${request.size}, ${request.quality.russianName()}")
    }

    private fun estimateVideo(request: AiGenerationRequest.Video): AiCostEstimate {
        require(request.seconds in setOf(4, 8, 12)) {
            "OpenAI поддерживает видео длительностью 4, 8 или 12 секунд"
        }
        val perSecond = when (request.model) {
            "sora-2" -> {
                require(request.size in setOf("1280x720", "720x1280")) {
                    "Sora 2 поддерживает выбранный экономный режим только в 720p"
                }
                0.10
            }

            "sora-2-pro" -> when (request.size) {
                "1280x720", "720x1280" -> 0.30
                "1792x1024", "1024x1792" -> 0.50
                "1920x1080", "1080x1920" -> 0.70
                else -> error("Неизвестна цена Sora 2 Pro для ${request.size}")
            }

            else -> error("Для модели ${request.model} не настроен безопасный расчёт стоимости")
        }
        return AiCostEstimate(
            usd = request.seconds * perSecond,
            description = "${request.seconds} сек., ${request.size}"
        )
    }

    private fun generateText(
        request: AiGenerationRequest.Text,
        onProgress: (Int, String) -> Unit
    ): AiGenerationResult.Text {
        onProgress(10, "Готовим текстовый запрос…")
        val body = buildJsonObject {
            put("model", request.model)
            put(
                "instructions",
                "Ты помощник программы HolyPresenter для христианских церквей. " +
                        "Отвечай на русском языке, ясно и уважительно. " +
                        "Не выдумывай цитаты из Библии: если точная формулировка не дана, " +
                        "предлагай пользователю проверить перевод."
            )
            put("input", request.prompt)
            put("max_output_tokens", request.maxOutputTokens)
        }
        val response = sendJson("/responses", body, Duration.ofMinutes(3))
        onProgress(90, "Обрабатываем ответ…")
        val root = json.parseToJsonElement(response).jsonObject
        val text = extractOutputText(root).trim()
        check(text.isNotBlank()) { "OpenAI вернул пустой текст" }
        val usage = parseUsage(root)
        val actualCost = usage.takeIf { it.inputTokens + it.outputTokens > 0 }
            ?.let { tokenCost(it, textPricing(request.model)) }
            ?: estimate(request).usd
        onProgress(100, "Готово")
        return AiGenerationResult.Text(id, text, actualCost, usage)
    }

    private fun generateImage(
        request: AiGenerationRequest.Image,
        onProgress: (Int, String) -> Unit
    ): AiGenerationResult.Media {
        onProgress(10, "Создаём изображение…")
        val body = buildJsonObject {
            put("model", request.model)
            put("prompt", request.prompt)
            put("quality", request.quality.name.lowercase())
            put("size", request.size)
            put("n", 1)
            put("output_format", "jpeg")
            put("output_compression", 90)
        }
        val response = sendJson("/images/generations", body, Duration.ofMinutes(5))
        val root = json.parseToJsonElement(response).jsonObject
        val encoded = root["data"]?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("b64_json")?.jsonPrimitive?.contentOrNull
            ?: error("OpenAI не вернул данные изображения")
        onProgress(85, "Сохраняем изображение…")
        val bytes = Base64.getDecoder().decode(encoded)
        val destination = generatedFile("ai-image", "jpg")
        writeAtomically(destination, bytes)
        bytes.fill(0)
        val usage = parseUsage(root)
        val actualCost = imageActualCost(root, request)
        onProgress(100, "Изображение готово")
        return AiGenerationResult.Media(
            providerId = id,
            filePath = destination.absolutePath,
            mimeType = "image/jpeg",
            kind = AiGenerationKind.IMAGE,
            costUsd = actualCost,
            usage = usage
        )
    }

    private suspend fun generateVideo(
        request: AiGenerationRequest.Video,
        onProgress: (Int, String) -> Unit
    ): AiGenerationResult.Media {
        onProgress(2, "Отправляем видео в очередь…")
        val createResponse = sendMultipart(
            path = "/videos",
            fields = mapOf(
                "model" to request.model,
                "prompt" to request.prompt,
                "seconds" to request.seconds.toString(),
                "size" to request.size
            ),
            timeout = Duration.ofMinutes(2)
        )
        val created = json.parseToJsonElement(createResponse).jsonObject
        val videoId = created.string("id") ?: error("OpenAI не вернул идентификатор видео")

        val deadline = System.nanoTime() + Duration.ofMinutes(12).toNanos()
        while (System.nanoTime() < deadline) {
            delay(2_000)
            val statusRoot = json.parseToJsonElement(
                sendGet("/videos/$videoId", Duration.ofSeconds(45))
            ).jsonObject
            val status = statusRoot.string("status")
            val progress = statusRoot["progress"]?.jsonPrimitive?.intOrNull ?: 0
            onProgress(progress.coerceIn(3, 95), "Создаём видео: $progress%")
            when (status) {
                "completed" -> break
                "failed" -> error(videoFailure(statusRoot))
            }
        }

        val finalStatus = json.parseToJsonElement(
            sendGet("/videos/$videoId", Duration.ofSeconds(45))
        ).jsonObject
        check(finalStatus.string("status") == "completed") {
            "Создание видео не завершилось за 12 минут"
        }
        onProgress(96, "Скачиваем видео…")
        val bytes = sendBytes("/videos/$videoId/content", Duration.ofMinutes(3))
        val destination = generatedFile("ai-video", "mp4")
        writeAtomically(destination, bytes)
        onProgress(100, "Видео готово")
        return AiGenerationResult.Media(
            providerId = id,
            filePath = destination.absolutePath,
            mimeType = "video/mp4",
            kind = AiGenerationKind.VIDEO,
            costUsd = estimate(request).usd
        )
    }

    private fun sendJson(path: String, body: JsonObject, timeout: Duration): String {
        val request = authorized(HttpRequest.newBuilder(apiUri(path)))
            .timeout(timeout)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString(), Charsets.UTF_8))
            .build()
        return sendString(request)
    }

    private fun sendGet(path: String, timeout: Duration): String {
        val request = authorized(HttpRequest.newBuilder(apiUri(path)))
            .timeout(timeout)
            .GET()
            .build()
        return sendString(request)
    }

    private fun sendMultipart(
        path: String,
        fields: Map<String, String>,
        timeout: Duration
    ): String {
        val boundary = "HolyPresenter-${UUID.randomUUID()}"
        val body = buildString {
            fields.forEach { (name, value) ->
                append("--").append(boundary).append("\r\n")
                append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n\r\n")
                append(value).append("\r\n")
            }
            append("--").append(boundary).append("--\r\n")
        }
        val request = authorized(HttpRequest.newBuilder(apiUri(path)))
            .timeout(timeout)
            .header("Content-Type", "multipart/form-data; boundary=$boundary")
            .POST(HttpRequest.BodyPublishers.ofString(body, Charsets.UTF_8))
            .build()
        return sendString(request)
    }

    private fun sendBytes(path: String, timeout: Duration): ByteArray {
        val request = authorized(HttpRequest.newBuilder(apiUri(path)))
            .timeout(timeout)
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
        if (response.statusCode() !in 200..299) {
            val errorBytes = response.body().copyOfRange(
                fromIndex = 0,
                toIndex = minOf(response.body().size, MAX_ERROR_BYTES)
            )
            checkSuccessful(response.statusCode(), errorBytes.toString(Charsets.UTF_8))
        }
        return response.body()
    }

    private fun sendString(request: HttpRequest): String {
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(Charsets.UTF_8))
        checkSuccessful(response.statusCode(), response.body())
        return response.body()
    }

    private fun authorized(builder: HttpRequest.Builder): HttpRequest.Builder {
        val key = apiKeyStore.load()
            ?: error("Добавьте персональный API-ключ OpenAI в настройках ИИ-помощника")
        return builder
            .header("Authorization", "Bearer $key")
            .header("Accept", "application/json")
            .header("User-Agent", "HolyPresenter/1.0")
    }

    private fun checkSuccessful(statusCode: Int, body: String) {
        if (statusCode in 200..299) return
        val apiMessage = runCatching {
            val root = json.parseToJsonElement(body).jsonObject
            root["error"]?.jsonObject?.string("message")
        }.getOrNull()?.take(600)
        val friendly = when (statusCode) {
            401 -> "API-ключ OpenAI недействителен или отозван"
            403 -> "Для аккаунта OpenAI недоступна выбранная модель"
            429 -> "Достигнут лимит OpenAI или на аккаунте недостаточно средств"
            else -> "Ошибка OpenAI ($statusCode)"
        }
        error(if (apiMessage.isNullOrBlank()) friendly else "$friendly: $apiMessage")
    }

    private fun extractOutputText(root: JsonObject): String {
        val parts = mutableListOf<String>()
        root["output"]?.jsonArray?.forEach { output ->
            output.jsonObject["content"]?.jsonArray?.forEach { content ->
                val item = content.jsonObject
                if (item.string("type") == "output_text") {
                    item.string("text")?.let(parts::add)
                }
            }
        }
        return parts.joinToString("\n")
    }

    private fun parseUsage(root: JsonObject): AiTokenUsage {
        val usage = root["usage"] as? JsonObject ?: return AiTokenUsage()
        val details = usage["input_tokens_details"] as? JsonObject
        return AiTokenUsage(
            inputTokens = usage.long("input_tokens"),
            cachedInputTokens = details?.long("cached_tokens") ?: 0,
            outputTokens = usage.long("output_tokens")
        )
    }

    private fun imageActualCost(
        root: JsonObject,
        request: AiGenerationRequest.Image
    ): Double {
        val usage = root["usage"] as? JsonObject ?: return estimate(request).usd
        val input = usage.long("input_tokens")
        val output = usage.long("output_tokens")
        val details = usage["input_tokens_details"] as? JsonObject
        val imageInput = details?.long("image_tokens") ?: 0
        val textInput = details?.long("text_tokens") ?: (input - imageInput).coerceAtLeast(0)
        return (textInput * 5.0 + imageInput * 8.0 + output * 30.0) / ONE_MILLION
    }

    private fun videoFailure(root: JsonObject): String {
        val error = root["error"] as? JsonObject
        return error?.string("message")?.let { "OpenAI не создал видео: ${it.take(500)}" }
            ?: "OpenAI не создал видео"
    }

    private fun textPricing(model: String): TextPricing = when (model) {
        "gpt-5.6-luna" -> TextPricing(0.10, 0.01, 0.60)
        "gpt-5.6-terra" -> TextPricing(1.00, 0.10, 6.00)
        "gpt-5.6-sol" -> TextPricing(2.50, 0.25, 15.00)
        else -> error("Для модели $model не настроен безопасный расчёт стоимости")
    }

    private fun tokenCost(usage: AiTokenUsage, pricing: TextPricing): Double =
        tokenCost(
            inputTokens = usage.inputTokens,
            cachedInputTokens = usage.cachedInputTokens,
            outputTokens = usage.outputTokens,
            pricing = pricing
        )

    private fun tokenCost(
        inputTokens: Long,
        cachedInputTokens: Long,
        outputTokens: Long,
        pricing: TextPricing
    ): Double {
        val cached = cachedInputTokens.coerceIn(0, inputTokens)
        val ordinary = (inputTokens - cached).coerceAtLeast(0)
        return (
                ordinary * pricing.inputPerMillion +
                        cached * pricing.cachedInputPerMillion +
                        outputTokens * pricing.outputPerMillion
                ) / ONE_MILLION
    }

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

    private fun apiUri(path: String): URI = URI.create("$API_ROOT$path")

    private fun JsonObject.string(name: String): String? =
        this[name]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.long(name: String): Long =
        this[name]?.jsonPrimitive?.longOrNull ?: 0L

    private data class TextPricing(
        val inputPerMillion: Double,
        val cachedInputPerMillion: Double,
        val outputPerMillion: Double
    )

    private companion object {
        const val API_ROOT = "https://api.openai.com/v1"
        const val ONE_MILLION = 1_000_000.0
        const val MAX_ERROR_BYTES = 8_192
        val FILE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

        fun AiImageQuality.russianName(): String = when (this) {
            AiImageQuality.LOW -> "низкое качество"
            AiImageQuality.MEDIUM -> "среднее качество"
            AiImageQuality.HIGH -> "высокое качество"
        }
    }
}
