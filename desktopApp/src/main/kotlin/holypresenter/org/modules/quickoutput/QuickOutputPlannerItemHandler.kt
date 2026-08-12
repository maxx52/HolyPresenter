package holypresenter.org.modules.quickoutput

import holypresenter.org.platform.api.planner.PlannerItem
import holypresenter.org.platform.api.planner.PlannerItemHandler
import holypresenter.org.platform.api.projection.ProjectionService
import holypresenter.org.platform.api.audio.AudioPlaybackService

internal class QuickOutputPlannerItemHandler(
    private val projectionService: ProjectionService,
    private val audioPlaybackService: AudioPlaybackService?
) : PlannerItemHandler {
    override val moduleId = "quick-output"

    override fun activate(item: PlannerItem): Boolean {
        if (item.reference.moduleId != moduleId) return false
        val state = QuickOutputStateCodec.decode(item.reference.itemId) ?: return false
        projectionService.show(state.toProjectionContent())
        state.audioPath?.let { audioPlaybackService?.play(it) }
        return true
    }
}
