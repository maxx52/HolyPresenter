package holypresenter.org.platform.api.presentation.theme

data class PresentationBackground(
    val type: PresentationBackgroundType,
    val path: String? = null,
    val color: Long? = null
)