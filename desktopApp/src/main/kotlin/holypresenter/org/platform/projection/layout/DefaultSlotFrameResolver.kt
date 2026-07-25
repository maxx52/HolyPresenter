package holypresenter.org.platform.projection.layout

import holypresenter.org.platform.api.presentation.SlotId
import holypresenter.org.platform.api.presentation.geometry.Frame

internal object DefaultSlotFrameResolver {
    private val mainFrame = Frame(
        x = 0.08f,
        y = 0.10f,
        width = 0.84f,
        height = 0.80f
    )

    fun resolve(
        slotId: SlotId
    ): Frame =
        when (slotId.value.lowercase()) {
            "main" -> mainFrame
            else -> Frame.Fullscreen
        }
}