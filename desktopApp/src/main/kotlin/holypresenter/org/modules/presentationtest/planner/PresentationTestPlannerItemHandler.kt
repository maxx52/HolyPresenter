package holypresenter.org.modules.presentationtest.planner

import holypresenter.org.modules.presentationtest.PresentationTestContent
import holypresenter.org.platform.api.planner.PlannerItem
import holypresenter.org.platform.api.planner.PlannerItemHandler
import holypresenter.org.platform.api.projection.ProjectionContent
import holypresenter.org.platform.api.projection.ProjectionService

internal class PresentationTestPlannerItemHandler(
    private val projectionService: ProjectionService
) : PlannerItemHandler {
    override val moduleId: String = "presentation-test"

    override fun activate(
        item: PlannerItem
    ): Boolean {
        if (item.reference.moduleId != moduleId) {
            return false
        }

        if (
            item.reference.itemId !=
            PresentationTestContent.ITEM_ID
        ) {
            return false
        }

        projectionService.show(
            ProjectionContent.Slide(
                presentation = PresentationTestContent.presentation,
                slideIndex = 0
            )
        )
        return true
    }
}