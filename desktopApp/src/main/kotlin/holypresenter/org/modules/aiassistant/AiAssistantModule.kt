package holypresenter.org.modules.aiassistant

import androidx.compose.runtime.Composable
import holypresenter.org.modules.aiassistant.ui.AiAssistantWorkspace
import holypresenter.org.platform.ai.AiAssistantStorage
import holypresenter.org.platform.ai.FreeTemplateAiProvider
import holypresenter.org.platform.ai.OllamaProvider
import holypresenter.org.platform.ai.OpenAiApiKeyStore
import holypresenter.org.platform.ai.OpenAiProvider
import holypresenter.org.platform.api.ai.AiProviderRegistry
import holypresenter.org.platform.api.module.HolyModule
import holypresenter.org.platform.api.module.ModuleContext
import holypresenter.org.platform.api.module.ModuleMetadata
import holypresenter.org.platform.api.planner.PlannerItemHandlerRegistry
import holypresenter.org.platform.api.projection.ProjectionService

class AiAssistantModule(
    private val storage: AiAssistantStorage,
    private val apiKeyStore: OpenAiApiKeyStore,
    private val freeTemplateProvider: FreeTemplateAiProvider,
    private val ollamaProvider: OllamaProvider,
    private val openAiProvider: OpenAiProvider
) : HolyModule {
    private lateinit var context: ModuleContext

    override val metadata = ModuleMetadata(
        id = "ai-assistant",
        name = "ИИ-помощник",
        version = "1.0.0",
        apiVersion = "0.6.0",
        author = "HolyPresenter",
        description = "Бесплатная локальная и облачная генерация материалов с контролем расходов",
        icon = "✨"
    )

    override fun onLoad(context: ModuleContext) {
        this.context = context
    }

    override fun onEnable(context: ModuleContext) {
        context.services.get(AiProviderRegistry::class)?.let { registry ->
            registry.register(freeTemplateProvider)
            registry.register(ollamaProvider)
            registry.register(openAiProvider)
        }
        val projection = context.services.get(ProjectionService::class) ?: return
        context.services.get(PlannerItemHandlerRegistry::class)?.register(
            AiAssistantPlannerItemHandler(projection)
        )
    }

    override fun onDisable() {
        context.services.get(AiProviderRegistry::class)?.let { registry ->
            registry.unregister(freeTemplateProvider.id)
            registry.unregister(ollamaProvider.id)
            registry.unregister(openAiProvider.id)
        }
        context.services.get(PlannerItemHandlerRegistry::class)?.unregister(metadata.id)
    }

    @Composable
    override fun Workspace() {
        AiAssistantWorkspace(
            context = context,
            storage = storage,
            apiKeyStore = apiKeyStore,
            ollamaProvider = ollamaProvider
        )
    }
}
