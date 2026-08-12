package holypresenter.org.modules.quickoutput

import androidx.compose.runtime.Composable
import holypresenter.org.modules.quickoutput.ui.QuickOutputWorkspace
import holypresenter.org.platform.api.module.HolyModule
import holypresenter.org.platform.api.module.ModuleContext
import holypresenter.org.platform.api.module.ModuleMetadata
import holypresenter.org.platform.api.planner.PlannerItemHandlerRegistry
import holypresenter.org.platform.api.projection.ProjectionService
import holypresenter.org.platform.api.audio.AudioPlaybackService

class QuickOutputModule : HolyModule {
    private lateinit var context: ModuleContext

    override val metadata = ModuleMetadata(
        id = "quick-output",
        name = "Быстрый вывод",
        version = "1.0.0",
        apiVersion = "0.6.0",
        author = "HolyPresenter",
        description = "Urgent text, image and video projection",
        icon = "⚡"
    )

    override fun onLoad(context: ModuleContext) { this.context = context }

    override fun onEnable(context: ModuleContext) {
        val registry = context.services.get(PlannerItemHandlerRegistry::class) ?: return
        val projector = context.services.get(ProjectionService::class) ?: return
        registry.register(
            QuickOutputPlannerItemHandler(
                projectionService = projector,
                audioPlaybackService = context.services.get(AudioPlaybackService::class)
            )
        )
    }

    override fun onDisable() { context.services.get(PlannerItemHandlerRegistry::class)?.unregister(metadata.id) }

    @Composable
    override fun Workspace() { QuickOutputWorkspace(context) }
}
