package holypresenter.org.modules.presentationtest

import androidx.compose.runtime.Composable
import holypresenter.org.modules.presentationtest.planner.PresentationTestPlannerItemHandler
import holypresenter.org.modules.presentationtest.ui.PresentationTestWorkspace
import holypresenter.org.platform.api.module.HolyModule
import holypresenter.org.platform.api.module.ModuleContext
import holypresenter.org.platform.api.module.ModuleMetadata
import holypresenter.org.platform.api.planner.PlannerItemHandlerRegistry
import holypresenter.org.platform.api.projection.ProjectionService

class PresentationTestModule : HolyModule {
    private lateinit var context: ModuleContext
    private var plannerItemHandlerRegistry: PlannerItemHandlerRegistry? = null

    override val metadata =
        ModuleMetadata(
            id = "presentation-test",
            name = "Presentation Test",
            version = "1.0.0",
            apiVersion = "0.6.0",
            author = "HolyPresenter",
            description = "Developer module for testing Presentation API",
            icon = "🧪"
        )

    override fun onLoad(
        context: ModuleContext
    ) {
        this.context = context
    }

    override fun onEnable(
        context: ModuleContext
    ) {
        val registry = context.services.get(PlannerItemHandlerRegistry::class)
        val projectionService = context.services.get(ProjectionService::class)

        if (
            registry == null ||
            projectionService == null
        ) {
            return
        }

        plannerItemHandlerRegistry = registry

        registry.register(
            PresentationTestPlannerItemHandler(
                projectionService = projectionService
            )
        )
    }

    override fun onDisable() {
        plannerItemHandlerRegistry?.unregister(metadata.id)
        plannerItemHandlerRegistry = null
    }

    @Composable
    override fun Workspace() {
        PresentationTestWorkspace(
            context = context
        )
    }
}