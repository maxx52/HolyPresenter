package holypresenter.org.platform.api.presentation

import holypresenter.org.platform.api.presentation.theme.PresentationTheme

data class Presentation(
    val id: String,
    val metadata: PresentationMetadata =
        PresentationMetadata(
            title = "Без названия"
        ),
    val theme: PresentationTheme,
    val slides: List<PresentationSlide> = emptyList()
)