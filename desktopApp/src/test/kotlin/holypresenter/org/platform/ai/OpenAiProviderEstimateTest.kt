package holypresenter.org.platform.ai

import holypresenter.org.platform.api.ai.AiGenerationRequest
import holypresenter.org.platform.api.ai.AiImageQuality
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OpenAiProviderEstimateTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun provider(): OpenAiProvider {
        val home = temporaryFolder.newFolder("provider-home-${System.nanoTime()}")
        return OpenAiProvider(
            apiKeyStore = OpenAiApiKeyStore(home, PassthroughProtector),
            applicationHome = home
        )
    }

    @Test
    fun estimate_usesDocumentedMediumLandscapeImagePrice() {
        val estimate = provider().estimate(
            AiGenerationRequest.Image(
                prompt = "Фон",
                model = "gpt-image-2",
                quality = AiImageQuality.MEDIUM,
                size = "1536x1024"
            )
        )

        assertEquals(0.041, estimate.usd, absoluteTolerance = 0.000_001)
    }

    @Test
    fun estimate_usesFourSecondEconomyVideoPrice() {
        val estimate = provider().estimate(
            AiGenerationRequest.Video(
                prompt = "Свет",
                model = "sora-2",
                seconds = 4,
                size = "1280x720"
            )
        )

        assertEquals(0.40, estimate.usd, absoluteTolerance = 0.000_001)
    }

    @Test
    fun estimate_rejectsUnknownModelInsteadOfUnderestimating() {
        assertFailsWith<IllegalStateException> {
            provider().estimate(
                AiGenerationRequest.Text(
                    prompt = "Текст",
                    model = "unknown-model",
                    maxOutputTokens = 1_000
                )
            )
        }
    }

    @Test
    fun economyTextEstimate_remainsBelowOneCent() {
        val estimate = provider().estimate(
            AiGenerationRequest.Text(
                prompt = "Подготовь короткое объявление о служении",
                model = "gpt-5.6-luna",
                maxOutputTokens = 1_200
            )
        )

        assertTrue(estimate.usd in 0.0..0.01)
    }

    private object PassthroughProtector : AiSecretProtector {
        override fun protect(value: ByteArray): ByteArray = value.copyOf()
        override fun unprotect(value: ByteArray): ByteArray = value.copyOf()
    }
}
