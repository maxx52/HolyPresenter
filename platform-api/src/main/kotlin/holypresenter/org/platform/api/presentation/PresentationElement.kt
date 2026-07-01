package holypresenter.org.platform.api.presentation

import holypresenter.org.platform.api.presentation.geometry.Frame

interface PresentationElement {
    val id: String
    val zIndex: Int
    val visible: Boolean
    val frame: Frame
}