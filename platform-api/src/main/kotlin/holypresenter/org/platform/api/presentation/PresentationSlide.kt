package holypresenter.org.platform.api.presentation

data class PresentationSlide(
    val id: String,
    val elements: List<PresentationElement> = emptyList()
)