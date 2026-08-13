package holypresenter.org.platform.ai

import holypresenter.org.platform.api.ai.AiGenerationRequest
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FreeTemplateAiProviderTest {
    private val provider = FreeTemplateAiProvider()

    @Test
    fun estimate_isAlwaysFreeForText() {
        val estimate = provider.estimate(
            AiGenerationRequest.Text("Объявление о встрече", "unused", 500)
        )

        assertEquals(0.0, estimate.usd)
        assertTrue(estimate.description.contains("бесплатно"))
    }

    @Test
    fun generate_formatsAnnouncementOffline() = runBlocking {
        val result = provider.generate(
            AiGenerationRequest.Text("Сделай объявление о молодёжной встрече", "unused", 500)
        )

        assertEquals(provider.id, result.providerId)
        assertEquals(0.0, result.costUsd)
        assertTrue(result.text.startsWith("ОБЪЯВЛЕНИЕ"))
        assertTrue(result.text.contains("молодёжной встрече"))
    }
}
