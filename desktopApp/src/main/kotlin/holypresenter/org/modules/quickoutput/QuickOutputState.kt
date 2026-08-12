package holypresenter.org.modules.quickoutput

import holypresenter.org.platform.api.presentation.*
import holypresenter.org.platform.api.presentation.element.TextElement
import holypresenter.org.platform.api.presentation.theme.*
import holypresenter.org.platform.api.projection.ProjectionContent
import java.util.Base64

internal data class QuickOutputState(val text: String, val mediaPath: String?, val mediaType: PresentationBackgroundType, val audioPath: String?) {
    fun toProjectionContent() = ProjectionContent.Slide(
        Presentation("quick-output", PresentationMetadata("Быстрый вывод"), PresentationTheme(PresentationBackground(mediaType, mediaPath, if (mediaType == PresentationBackgroundType.COLOR) 0xFF000000 else null), PresentationTextStyle(fontSize = 64, textColor = 0xFFFFFFFF, bold = true), PresentationOverlay(mediaType != PresentationBackgroundType.COLOR)), listOf(PresentationSlide("quick-output-slide", listOf(TextElement("quick-output-text", SlotId("main"), text))))), 0
    )
}

internal object QuickOutputStateCodec {
    fun encode(state: QuickOutputState): String = Base64.getUrlEncoder().encodeToString(listOf(state.text, state.mediaPath.orEmpty(), state.mediaType.name, state.audioPath.orEmpty()).joinToString("\u001F").toByteArray())
    fun decode(value: String): QuickOutputState? = runCatching {
        val p = String(Base64.getUrlDecoder().decode(value)).split("\u001F")
        QuickOutputState(p[0], p[1].ifBlank { null }, PresentationBackgroundType.valueOf(p[2]), p[3].ifBlank { null })
    }.getOrNull()
}
