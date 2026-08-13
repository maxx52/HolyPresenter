package holypresenter.org.modules.aiassistant

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
import holypresenter.org.platform.api.projection.ProjectionContent
import java.util.Base64

internal data class AiAssistantPlanState(
    val text: String,
    val mediaPath: String?,
    val mediaType: PresentationBackgroundType
) {
    fun toProjectionContent(): ProjectionContent.Slide = ProjectionContent.Slide(
        presentation = Presentation(
            id = "ai-assistant",
            metadata = PresentationMetadata("ИИ-помощник"),
            theme = PresentationTheme(
                background = PresentationBackground(
                    type = mediaType,
                    path = mediaPath,
                    color = if (mediaType == PresentationBackgroundType.COLOR) {
                        0xFF10253FL
                    } else {
                        null
                    }
                ),
                textStyle = PresentationTextStyle(
                    fontSize = 64,
                    autoSize = true,
                    minFontSize = 24,
                    textColor = 0xFFFFFFFF,
                    bold = true
                ),
                overlay = PresentationOverlay(
                    enabled = mediaType != PresentationBackgroundType.COLOR
                )
            ),
            slides = listOf(
                PresentationSlide(
                    id = "ai-assistant-slide",
                    elements = listOf(
                        TextElement(
                            id = "ai-assistant-text",
                            slot = SlotId("main"),
                            text = text
                        )
                    )
                )
            )
        ),
        slideIndex = 0
    )
}

internal object AiAssistantPlanStateCodec {
    private const val SEPARATOR = "\u001F"

    fun encode(state: AiAssistantPlanState): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(
            listOf(
                state.text,
                state.mediaPath.orEmpty(),
                state.mediaType.name
            ).joinToString(SEPARATOR).toByteArray(Charsets.UTF_8)
        )

    fun decode(value: String): AiAssistantPlanState? = runCatching {
        val parts = Base64.getUrlDecoder().decode(value)
            .toString(Charsets.UTF_8)
            .split(SEPARATOR)
        AiAssistantPlanState(
            text = parts[0],
            mediaPath = parts[1].ifBlank { null },
            mediaType = PresentationBackgroundType.valueOf(parts[2])
        )
    }.getOrNull()
}
