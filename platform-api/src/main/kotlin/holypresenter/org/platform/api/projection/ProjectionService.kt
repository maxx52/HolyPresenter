package holypresenter.org.platform.api.projection

import kotlinx.coroutines.flow.StateFlow

interface ProjectionService {
    val state: StateFlow<ProjectionState>

    fun show(
        content: ProjectionContent
    )

    fun clear()
    fun close()
}