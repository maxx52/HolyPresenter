package holypresenter.org.platform.api.presentation

data class Presentation(
    val id: String,
    val metadata: PresentationMetadata =
        PresentationMetadata(
            title = "Без названия"
        ),
    val slides: List<PresentationSlide> = emptyList()
)