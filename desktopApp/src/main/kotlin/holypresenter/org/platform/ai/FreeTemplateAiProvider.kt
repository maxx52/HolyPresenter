package holypresenter.org.platform.ai

import holypresenter.org.platform.api.ai.AiCostEstimate
import holypresenter.org.platform.api.ai.AiGenerationKind
import holypresenter.org.platform.api.ai.AiGenerationRequest
import holypresenter.org.platform.api.ai.AiGenerationResult
import holypresenter.org.platform.api.ai.AiProvider
import holypresenter.org.platform.api.ai.AiTokenUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A deterministic offline fallback that remains useful when no neural model is installed.
 */
class FreeTemplateAiProvider : AiProvider {
    override val id: String = "free-templates"
    override val displayName: String = "Бесплатные шаблоны"
    override val supportedKinds: Set<AiGenerationKind> = setOf(AiGenerationKind.TEXT)

    override fun estimate(request: AiGenerationRequest): AiCostEstimate {
        require(request is AiGenerationRequest.Text) {
            "Бесплатные шаблоны поддерживают только текст"
        }
        return AiCostEstimate(0.0, "бесплатно · без интернета")
    }

    override suspend fun generate(
        request: AiGenerationRequest,
        onProgress: (percent: Int, message: String) -> Unit
    ): AiGenerationResult.Text = withContext(Dispatchers.Default) {
        require(request is AiGenerationRequest.Text) {
            "Бесплатные шаблоны поддерживают только текст"
        }
        val prompt = request.prompt.trim()
        require(prompt.isNotBlank()) { "Введите содержание для оформления" }
        onProgress(30, "Подбираем бесплатный шаблон…")
        val text = formatTemplate(prompt)
        onProgress(100, "Шаблон готов")
        AiGenerationResult.Text(
            providerId = id,
            text = text,
            costUsd = 0.0,
            usage = AiTokenUsage()
        )
    }

    internal fun formatTemplate(prompt: String): String {
        val normalized = prompt
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return when {
            normalized.contains("план презентации", ignoreCase = true) -> """
                ПЛАН ПРЕЗЕНТАЦИИ

                1. Титульный слайд
                ${normalized.take(180)}

                2. Главная мысль
                Сформулируйте одну короткую мысль для зрителей.

                3. Основная информация
                Оставьте не более трёх коротких пунктов.

                4. Следующий шаг
                Чётко укажите, что нужно сделать или запомнить.

                5. Завершение
                Добавьте короткое приглашение или благодарность.
            """.trimIndent()

            normalized.contains("объяв", ignoreCase = true) ||
                    normalized.contains("приглаш", ignoreCase = true) -> """
                ОБЪЯВЛЕНИЕ

                ${normalized.take(500)}

                Будем рады видеть вас!
            """.trimIndent()

            else -> """
                ИНФОРМАЦИЯ

                ${normalized.take(700)}
            """.trimIndent()
        }
    }
}
