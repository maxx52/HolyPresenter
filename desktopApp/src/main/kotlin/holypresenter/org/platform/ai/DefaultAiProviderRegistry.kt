package holypresenter.org.platform.ai

import holypresenter.org.platform.api.ai.AiGenerationKind
import holypresenter.org.platform.api.ai.AiProvider
import holypresenter.org.platform.api.ai.AiProviderRegistry
import java.util.concurrent.CopyOnWriteArrayList

class DefaultAiProviderRegistry : AiProviderRegistry {
    private val registered = CopyOnWriteArrayList<AiProvider>()

    override fun register(provider: AiProvider) {
        unregister(provider.id)
        registered += provider
    }

    override fun unregister(providerId: String) {
        registered.removeIf { it.id == providerId }
    }

    override fun providers(kind: AiGenerationKind?): List<AiProvider> =
        registered
            .filter { provider -> kind == null || kind in provider.supportedKinds }
            .sortedBy(AiProvider::displayName)

    override fun provider(providerId: String): AiProvider? =
        registered.firstOrNull { it.id == providerId }
}
