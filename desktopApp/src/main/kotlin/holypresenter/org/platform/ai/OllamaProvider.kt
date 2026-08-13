package holypresenter.org.platform.ai

import holypresenter.org.platform.api.ai.AiCostEstimate
import holypresenter.org.platform.api.ai.AiGenerationKind
import holypresenter.org.platform.api.ai.AiGenerationRequest
import holypresenter.org.platform.api.ai.AiGenerationResult
import holypresenter.org.platform.api.ai.AiProvider
import holypresenter.org.platform.api.ai.AiTokenUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.net.ConnectException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

sealed interface OllamaStatus {
    data object Checking : OllamaStatus
    data object NotRunning : OllamaStatus
    data object ModelMissing : OllamaStatus
    data object Ready : OllamaStatus
    data class Failed(val message: String) : OllamaStatus
}

class OllamaProvider(
    private val apiRoot: URI = URI.create("http://127.0.0.1:11434/api"),
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build(),
    val model: String = DEFAULT_MODEL
) : AiProvider {
    override val id: String = "ollama-local"
    override val displayName: String = "Локальная нейросеть"
    override val supportedKinds: Set<AiGenerationKind> = setOf(AiGenerationKind.TEXT)

    private val json = Json { ignoreUnknownKeys = true }

    override fun estimate(request: AiGenerationRequest): AiCostEstimate {
        require(request is AiGenerationRequest.Text) {
            "Локальная нейросеть поддерживает только текст"
        }
        return AiCostEstimate(0.0, "бесплатно · локально · $model")
    }

    suspend fun status(): OllamaStatus = withContext(Dispatchers.IO) {
        runCatching {
            val request = HttpRequest.newBuilder(endpoint("/tags"))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build()
            val response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(Charsets.UTF_8)
            )
            if (response.statusCode() !in 200..299) {
                return@runCatching OllamaStatus.Failed("Ollama вернула ошибку ${response.statusCode()}")
            }
            val models = json.parseToJsonElement(response.body())
                .jsonObject["models"]?.jsonArray.orEmpty()
            val installed = models.any { item ->
                val objectValue = item.jsonObject
                val name = objectValue["model"]?.jsonPrimitive?.contentOrNull
                    ?: objectValue["name"]?.jsonPrimitive?.contentOrNull
                name == model || name == "$model:latest"
            }
            if (installed) OllamaStatus.Ready else OllamaStatus.ModelMissing
        }.getOrElse { throwable ->
            if (throwable.isConnectionFailure()) OllamaStatus.NotRunning
            else OllamaStatus.Failed(throwable.message ?: "Не удалось проверить Ollama")
        }
    }

    suspend fun installModel(
        onProgress: (percent: Int, message: String) -> Unit = { _, _ -> }
    ): OllamaStatus = withContext(Dispatchers.IO) {
        onProgress(1, "Начинаем загрузку $model…")
        val body = buildJsonObject {
            put("model", model)
            put("stream", true)
        }
        val request = HttpRequest.newBuilder(endpoint("/pull"))
            .timeout(Duration.ofMinutes(45))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString(), Charsets.UTF_8))
            .build()
        runCatching {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
            if (response.statusCode() !in 200..299) {
                response.body().close()
                error("Ollama не смогла загрузить модель (${response.statusCode()})")
            }
            response.body().bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.filter(String::isNotBlank).forEach { line ->
                    val update = json.parseToJsonElement(line).jsonObject
                    update["error"]?.jsonPrimitive?.contentOrNull?.let(::error)
                    val total = update["total"]?.jsonPrimitive?.longOrNull ?: 0L
                    val completed = update["completed"]?.jsonPrimitive?.longOrNull ?: 0L
                    val percent = if (total > 0L) {
                        ((completed * 100L) / total).toInt().coerceIn(2, 99)
                    } else {
                        2
                    }
                    val statusText = update["status"]?.jsonPrimitive?.contentOrNull
                        ?: "Загружаем модель…"
                    onProgress(percent, statusText)
                }
            }
            onProgress(100, "Модель установлена")
            OllamaStatus.Ready
        }.getOrElse { throwable ->
            if (throwable.isConnectionFailure()) OllamaStatus.NotRunning
            else OllamaStatus.Failed(throwable.message ?: "Не удалось установить модель")
        }
    }

    override suspend fun generate(
        request: AiGenerationRequest,
        onProgress: (percent: Int, message: String) -> Unit
    ): AiGenerationResult.Text = withContext(Dispatchers.IO) {
        require(request is AiGenerationRequest.Text) {
            "Локальная нейросеть поддерживает только текст"
        }
        require(request.prompt.isNotBlank()) { "Введите запрос для ИИ-помощника" }
        onProgress(5, "Запускаем локальную нейросеть…")
        val body = buildJsonObject {
            put("model", model)
            put("stream", false)
            put("think", false)
            put("keep_alive", "2m")
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "system")
                    put(
                        "content",
                        "Ты помощник программы HolyPresenter для христианских церквей. " +
                            "Отвечай на русском языке, ясно, кратко и уважительно. " +
                            "Не выдумывай цитаты из Библии: проси проверить точный перевод."
                    )
                })
                add(buildJsonObject {
                    put("role", "user")
                    put("content", request.prompt)
                })
            })
            put("options", buildJsonObject {
                put("num_predict", request.maxOutputTokens.coerceIn(128, 2_048))
                put("num_ctx", 4_096)
                put("temperature", 0.7)
            })
        }
        val httpRequest = HttpRequest.newBuilder(endpoint("/chat"))
            .timeout(Duration.ofMinutes(10))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString(), Charsets.UTF_8))
            .build()
        val response = runCatching {
            httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(Charsets.UTF_8))
        }.getOrElse { throwable ->
            if (throwable.isConnectionFailure()) {
                error("Ollama не запущена. Установите и запустите её в настройках бесплатной нейросети.")
            }
            throw throwable
        }
        if (response.statusCode() !in 200..299) {
            val apiError = runCatching {
                json.parseToJsonElement(response.body()).jsonObject["error"]
                    ?.jsonPrimitive?.contentOrNull
            }.getOrNull()
            if (response.statusCode() == 404) {
                error("Модель $model не установлена. Нажмите «Установить модель».")
            }
            error(apiError ?: "Ошибка Ollama (${response.statusCode()})")
        }
        onProgress(90, "Оформляем ответ…")
        val root = json.parseToJsonElement(response.body()).jsonObject
        val text = root["message"]?.jsonObject
            ?.get("content")?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        check(text.isNotBlank()) { "Локальная нейросеть вернула пустой ответ" }
        val usage = AiTokenUsage(
            inputTokens = root["prompt_eval_count"]?.jsonPrimitive?.longOrNull ?: 0L,
            outputTokens = root["eval_count"]?.jsonPrimitive?.longOrNull ?: 0L
        )
        onProgress(100, "Готово — запрос выполнен локально")
        AiGenerationResult.Text(
            providerId = id,
            text = text,
            costUsd = 0.0,
            usage = usage
        )
    }

    private fun endpoint(path: String): URI = apiRoot.resolve(
        apiRoot.path.trimEnd('/') + "/" + path.trimStart('/')
    )

    private fun Throwable.isConnectionFailure(): Boolean =
        this is ConnectException || cause is ConnectException ||
            javaClass.simpleName.contains("ConnectTimeout", ignoreCase = true)

    companion object {
        const val DEFAULT_MODEL: String = "qwen3:1.7b"
        const val DOWNLOAD_URL: String = "https://ollama.com/download"
        const val MODEL_SIZE_LABEL: String = "около 1,4 ГБ"
    }
}
