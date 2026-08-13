package holypresenter.org.modules.aiassistant

import holypresenter.org.platform.api.planner.PlannerItem
import holypresenter.org.platform.api.planner.PlannerItemHandler
import holypresenter.org.platform.api.projection.ProjectionService

internal class AiAssistantPlannerItemHandler(
    private val projectionService: ProjectionService
) : PlannerItemHandler {
    override val moduleId: String = "ai-assistant"

    override fun activate(item: PlannerItem): Boolean {
        if (item.reference.moduleId != moduleId) return false
        val state = AiAssistantPlanStateCodec.decode(item.reference.itemId) ?: return false
        projectionService.show(state.toProjectionContent())
        return true
    }
}
