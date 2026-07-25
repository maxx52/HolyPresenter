package holypresenter.org.platform.api.projection

import holypresenter.org.platform.api.presentation.Presentation
import holypresenter.org.platform.api.presentation.PresentationSlide
import holypresenter.org.platform.api.presentation.SlotId
import holypresenter.org.platform.api.presentation.element.TextElement
import holypresenter.org.platform.api.presentation.theme.PresentationBackground
import holypresenter.org.platform.api.presentation.theme.PresentationOverlay
import holypresenter.org.platform.api.presentation.theme.PresentationTheme
import holypresenter.org.platform.api.presentation.theme.PresentationBackgroundType as ThemeBackgroundType

sealed interface ProjectionContent {
    data object Empty : ProjectionContent
    data object BlackScreen : ProjectionContent
    data object Logo : ProjectionContent

    data class Slide(
        val presentation: Presentation,
        val slideIndex: Int = 0
    ) : ProjectionContent {
        val slide: PresentationSlide?
            get() = presentation.slides.getOrNull(slideIndex)

        /*
         * Временный совместимый API.
         *
         * Он позволяет старым модулям, собранным против прежней
         * модели ProjectionContent. Slide, продолжить работу до
         * их перевода на Presentation.
         */

        @Deprecated(
            message = "Use presentation instead",
            replaceWith = ReplaceWith("presentation.id")
        )
        val presentationId: String
            get() = presentation.id

        @Deprecated(
            message = "Use slide instead"
        )
        val slideId: String
            get() = slide?.id.orEmpty()

        @Deprecated(
            message = "Read TextElement from slide"
        )
        val text: String
            get() = slide
                ?.elements
                ?.filterIsInstance<TextElement>()
                ?.joinToString("\n") { it.text }
                .orEmpty()

        @Deprecated(
            message = "Use presentation.theme.background.path"
        )
        val backgroundPath: String?
            get() = presentation.theme.background.path

        @Deprecated(
            message = "Use presentation.theme.background.type"
        )
        val backgroundType: ProjectionBackgroundType
            get() = when (
                presentation.theme.background.type
            ) {
                ThemeBackgroundType.COLOR ->
                    ProjectionBackgroundType.COLOR

                ThemeBackgroundType.IMAGE ->
                    ProjectionBackgroundType.IMAGE

                ThemeBackgroundType.VIDEO ->
                    ProjectionBackgroundType.VIDEO
            }

        @Deprecated(
            message = "Use Slide(presentation, slideIndex)"
        )
        constructor(
            presentationId: String,
            slideId: String,
            text: String = "",
            backgroundPath: String? = null,
            backgroundType: ProjectionBackgroundType =
                ProjectionBackgroundType.NONE
        ) : this(
            presentation = createLegacyPresentation(
                presentationId = presentationId,
                slideId = slideId,
                text = text,
                backgroundPath = backgroundPath,
                backgroundType = backgroundType
            ),
            slideIndex = 0
        )
    }
}

private fun createLegacyPresentation(
    presentationId: String,
    slideId: String,
    text: String,
    backgroundPath: String?,
    backgroundType: ProjectionBackgroundType
): Presentation {
    val themeBackgroundType =
        when (backgroundType) {
            ProjectionBackgroundType.NONE,
            ProjectionBackgroundType.COLOR ->
                ThemeBackgroundType.COLOR

            ProjectionBackgroundType.IMAGE ->
                ThemeBackgroundType.IMAGE

            ProjectionBackgroundType.VIDEO ->
                ThemeBackgroundType.VIDEO
        }

    return Presentation(
        id = presentationId,
        theme = PresentationTheme(
            background = PresentationBackground(
                type = themeBackgroundType,
                path = backgroundPath,
                color = when (backgroundType) {
                    ProjectionBackgroundType.NONE,
                    ProjectionBackgroundType.COLOR ->
                        0xFF000000

                    else -> null
                }
            ),
            overlay = PresentationOverlay(false)
        ),
        slides = listOf(
            PresentationSlide(
                id = slideId,
                elements = listOf(
                    TextElement(
                        id = "$slideId-text",
                        slot = SlotId("main"),
                        text = text
                    )
                )
            )
        )
    )
}