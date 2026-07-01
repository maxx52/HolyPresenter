package holypresenter.org.platform.api.presentation

interface PresentationElement {
    val id: String
    val slot: SlotId
    val zIndex: Int
    val visible: Boolean
}