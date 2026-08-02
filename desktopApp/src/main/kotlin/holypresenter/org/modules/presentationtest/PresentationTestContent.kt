package holypresenter.org.modules.presentationtest

import holypresenter.org.platform.api.presentation.Presentation
import holypresenter.org.platform.api.presentation.PresentationMetadata
import holypresenter.org.platform.api.presentation.PresentationSlide
import holypresenter.org.platform.api.presentation.SlotId
import holypresenter.org.platform.api.presentation.element.TextElement
import holypresenter.org.platform.api.presentation.theme.PresentationBackground
import holypresenter.org.platform.api.presentation.theme.PresentationBackgroundType
import holypresenter.org.platform.api.presentation.theme.PresentationOverlay
import holypresenter.org.platform.api.presentation.theme.PresentationTextStyle
import holypresenter.org.platform.api.presentation.theme.PresentationTheme

internal object PresentationTestContent {
    const val ITEM_ID = "test-presentation"
    const val TITLE = "Тестовая презентация"

    val presentation =
        Presentation(
            id = ITEM_ID,
            metadata = PresentationMetadata(
                title = TITLE
            ),
            theme = PresentationTheme(
                background =
                    PresentationBackground(
                        type = PresentationBackgroundType.COLOR,
                        color = 0xFF202124
                    ),
                textStyle =
                    PresentationTextStyle(
                        fontSize = 64,
                        textColor = 0xFFFFFFFF,
                        bold = true,
                        outlineEnabled = true,
                        shadowEnabled = true
                    ),
                overlay =
                    PresentationOverlay(
                        enabled = false
                    )
            ),
            slides = listOf(
                PresentationSlide(
                    id = "test-slide-1",
                    elements = listOf(
                        TextElement(
                            id = "test-text-1",
                            slot = SlotId("main"),
                            text = "HolyPresenter\nработает!"
                        )
                    )
                ),
                PresentationSlide(
                    id = "test-slide-2",
                    elements = listOf(
                        TextElement(
                            id = "test-text-2",
                            slot = SlotId("main"),
                            text = "Второй слайд\nиз Presentation"
                        )
                    )
                )
            )
        )
}