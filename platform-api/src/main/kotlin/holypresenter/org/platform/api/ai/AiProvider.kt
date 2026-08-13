package holypresenter.org.platform.api.ai

import holypresenter.org.platform.api.services.HolyService

enum class AiGenerationKind {
    TEXT,
    IMAGE,
    VIDEO
}

enum class AiImageQuality {
    LOW,
    MEDIUM,
    HIGH
}

sealed interface AiGenerationRequest {
    val prompt: String

    data class Text(
        override val prompt: String,
        val model: String,
        val maxOutputTokens: Int
    ) : AiGenerationRequest

    data class Image(
        override val prompt: String,
        val model: String,
        val quality: AiImageQuality,
        val size: String
    ) : AiGenerationRequest

    data class Video(
        override val prompt: String,
        val model: String,
        val seconds: Int,
        val size: String
    ) : AiGenerationRequest
}

data class AiCostEstimate(
    val usd: Double,
    val description: String
)

data class AiTokenUsage(
    val inputTokens: Long = 0,
    val cachedInputTokens: Long = 0,
    val outputTokens: Long = 0
)

sealed interface AiGenerationResult {
    val providerId: String
    val costUsd: Double
    val usage: AiTokenUsage

    data class Text(
        override val providerId: String,
        val text: String,
        override val costUsd: Double,
        override val usage: AiTokenUsage
    ) : AiGenerationResult

    data class Media(
        override val providerId: String,
        val filePath: String,
        val mimeType: String,
        val kind: AiGenerationKind,
        override val costUsd: Double,
        override val usage: AiTokenUsage = AiTokenUsage()
    ) : AiGenerationResult
}

/**
 * Extension point for built-in and marketplace AI providers.
 *
 * Implementations must perform network or heavy work outside the UI thread.
 */
interface AiProvider {
    val id: String
    val displayName: String
    val supportedKinds: Set<AiGenerationKind>

    fun estimate(request: AiGenerationRequest): AiCostEstimate

    suspend fun generate(
        request: AiGenerationRequest,
        onProgress: (percent: Int, message: String) -> Unit = { _, _ -> }
    ): AiGenerationResult
}

interface AiProviderRegistry : HolyService {
    override val id: String
        get() = "ai-provider-registry"

    fun register(provider: AiProvider)
    fun unregister(providerId: String)
    fun providers(kind: AiGenerationKind? = null): List<AiProvider>
    fun provider(providerId: String): AiProvider?
}
