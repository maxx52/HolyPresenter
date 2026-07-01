package holypresenter.org.platform.api.presentation

data class Presentation(
    val id: String,
    val slides: List<PresentationSlide> = emptyList()
)