package holypresenter.org.modules.quickoutput

import holypresenter.org.platform.api.presentation.*
import holypresenter.org.platform.api.presentation.element.TextElement
import holypresenter.org.platform.api.presentation.theme.*
import holypresenter.org.platform.api.projection.ProjectionContent
import java.util.Base64

internal data class QuickOutputState(val text: String, val mediaPath: String?, val mediaType: PresentationBackgroundType, val audioPath: String?) {
    fun toProjectionContent() = ProjectionContent.Slide(
        presentation = Presentation(
            id = "quick-output",
            metadata = PresentationMetadata("Быстрый вывод"),
            theme = PresentationTheme(
                background = PresentationBackground(
                    type = mediaType,
                    path = mediaPath,
                    color = if (mediaType == PresentationBackgroundType.COLOR) {
                        0xFF000000
                    } else {
                        null
                    }
                ),
                textStyle = PresentationTextStyle(
                    fontSize = 64,
                    textColor = 0xFFFFFFFF,
                    bold = true
                ),
                overlay = PresentationOverlay(
                    enabled = mediaType != PresentationBackgroundType.COLOR
                )
            ),
            slides = listOf(
                PresentationSlide(
                    id = "quick-output-slide",
                    elements = listOf(
                        TextElement(
                            id = "quick-output-text",
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

internal object QuickOutputStateCodec {
    fun encode(state: QuickOutputState): String = Base64.getUrlEncoder().encodeToString(listOf(state.text, state.mediaPath.orEmpty(), state.mediaType.name, state.audioPath.orEmpty()).joinToString("\u001F").toByteArray())
    fun decode(value: String): QuickOutputState? = runCatching {
        val p = String(Base64.getUrlDecoder().decode(value)).split("\u001F")
        QuickOutputState(p[0], p[1].ifBlank { null }, PresentationBackgroundType.valueOf(p[2]), p[3].ifBlank { null })
    }.getOrNull()
}
