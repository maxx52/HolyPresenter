package holypresenter.org.platform.planner

import holypresenter.org.platform.api.planner.PlannerItem
import holypresenter.org.platform.api.planner.PlannerItemHandler
import holypresenter.org.platform.api.planner.PlannerItemHandlerRegistry

internal class DefaultPlannerItemHandlerRegistry : PlannerItemHandlerRegistry {
    private val handlers = mutableMapOf<String, PlannerItemHandler>()

    override fun register(
        handler: PlannerItemHandler
    ) {
        require(
            handler.moduleId.isNotBlank()
        ) {
            "Planner item handler moduleId must not be blank"
        }
        handlers[handler.moduleId] = handler
    }

    override fun unregister(moduleId: String) {
        handlers.remove(moduleId)
    }

    override fun activate(item: PlannerItem): Boolean {
        val handler = handlers[item.reference.moduleId] ?: return false
        return handler.activate(item)
    }
}