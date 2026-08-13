package holypresenter.org.platform.ai

import holypresenter.org.platform.api.ai.AiGenerationKind
import holypresenter.org.platform.api.ai.AiGenerationRequest
import holypresenter.org.platform.api.ai.AiImageQuality
import kotlinx.serialization.Serializable

@Serializable
data class AiAssistantSettings(
    val providerId: String = "openai",
    val textModel: String = "gpt-5.6-luna",
    val imageModel: String = "gpt-image-2",
    val videoModel: String = "sora-2",
    val maxOutputTokens: Int = 1_200,
    val imageQuality: AiImageQuality = AiImageQuality.MEDIUM,
    val imageSize: String = "1536x1024",
    val videoSeconds: Int = 4,
    val videoSize: String = "1280x720",
    val monthlyLimitUsd: Double = 10.0
) {
    fun request(kind: AiGenerationKind, prompt: String): AiGenerationRequest = when (kind) {
        AiGenerationKind.TEXT -> AiGenerationRequest.Text(
            prompt = prompt,
            model = textModel,
            maxOutputTokens = maxOutputTokens.coerceIn(128, 16_000)
        )

        AiGenerationKind.IMAGE -> AiGenerationRequest.Image(
            prompt = prompt,
            model = imageModel,
            quality = imageQuality,
            size = imageSize
        )

        AiGenerationKind.VIDEO -> AiGenerationRequest.Video(
            prompt = prompt,
            model = videoModel,
            seconds = videoSeconds.coerceIn(4, 12),
            size = videoSize
        )
    }
}

@Serializable
data class AiUsageLedger(
    val monthCostsUsd: Map<String, Double> = emptyMap()
)
