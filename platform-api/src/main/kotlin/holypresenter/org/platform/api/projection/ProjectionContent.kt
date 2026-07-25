package holypresenter.org.platform.api.projection

sealed interface ProjectionContent {
    data object Empty : ProjectionContent
    data object BlackScreen : ProjectionContent
    data object Logo : ProjectionContent
    data class Slide(
        val presentationId: String,
        val slideId: String,
        val text: String = "",
        val backgroundPath: String? = null,
        val backgroundType: ProjectionBackgroundType =
            ProjectionBackgroundType.NONE
    ) : ProjectionContent
}