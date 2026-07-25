package holypresenter.org.platform.projection

import holypresenter.org.platform.api.projection.ProjectionContent
import holypresenter.org.platform.api.projection.ProjectionService
import holypresenter.org.platform.api.projection.ProjectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DefaultProjectionService : ProjectionService {
    private val mutableState =
        MutableStateFlow(ProjectionState())

    override val state: StateFlow<ProjectionState> =
        mutableState.asStateFlow()

    private val projectionWindow =
        ProjectionWindow(
            onClose = ::close
        )

    override fun show(
        content: ProjectionContent
    ) {
        projectionWindow.show(content)

        mutableState.value = ProjectionState(
            content = content,
            visible = true
        )
    }

    override fun clear() {
        projectionWindow.show(
            ProjectionContent.Empty
        )

        mutableState.value = ProjectionState(
            content = ProjectionContent.Empty,
            visible = true
        )
    }

    override fun close() {
        projectionWindow.close()

        mutableState.value = ProjectionState(
            content = ProjectionContent.Empty,
            visible = false
        )
    }
}