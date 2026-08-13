package holypresenter.org.platform.ai

import holypresenter.org.platform.api.ai.AiCostEstimate
import holypresenter.org.platform.api.ai.AiGenerationKind
import holypresenter.org.platform.api.ai.AiGenerationRequest
import holypresenter.org.platform.api.ai.AiGenerationResult
import holypresenter.org.platform.api.ai.AiProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DefaultAiProviderRegistryTest {
    @Test
    fun register_replacesProviderWithSameIdAndFiltersCapabilities() {
        val registry = DefaultAiProviderRegistry()
        registry.register(FakeProvider("custom", "Первый", setOf(AiGenerationKind.TEXT)))
        registry.register(FakeProvider("custom", "Второй", setOf(AiGenerationKind.IMAGE)))

        assertEquals("Второй", registry.provider("custom")?.displayName)
        assertEquals(emptyList(), registry.providers(AiGenerationKind.TEXT))
        assertEquals(listOf("custom"), registry.providers(AiGenerationKind.IMAGE).map(AiProvider::id))

        registry.unregister("custom")
        assertNull(registry.provider("custom"))
    }

    private data class FakeProvider(
        override val id: String,
        override val displayName: String,
        override val supportedKinds: Set<AiGenerationKind>
    ) : AiProvider {
        override fun estimate(request: AiGenerationRequest) = AiCostEstimate(0.0, "test")

        override suspend fun generate(
            request: AiGenerationRequest,
            onProgress: (Int, String) -> Unit
        ): AiGenerationResult = error("Not used")
    }
}
