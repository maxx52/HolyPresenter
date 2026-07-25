package holypresenter.org.platform.api.projection

data class ProjectionState(
    val content: ProjectionContent = ProjectionContent.Empty,
    val visible: Boolean = false
)