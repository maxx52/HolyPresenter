package holypresenter.org.platform.api.presentation.element

import holypresenter.org.platform.api.presentation.PresentationElement
import holypresenter.org.platform.api.presentation.SlotId

data class TextElement(
    override val id: String,
    override val slot: SlotId,
    override val zIndex: Int = 0,
    override val visible: Boolean = true,

    val text: String
) : PresentationElement