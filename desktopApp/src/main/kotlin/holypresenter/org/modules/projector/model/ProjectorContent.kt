package holypresenter.org.modules.projector.model

sealed interface ProjectorContent {
    data object Empty : ProjectorContent

    data class Text(
        val value: String
    ) : ProjectorContent

    data object BlackScreen : ProjectorContent
}