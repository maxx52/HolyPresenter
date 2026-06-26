package holypresenter.org.platform.layout

data class DockPanelLayout(
    val id: String,
    val position: String,
    val visible: Boolean = true,
    val pinned: Boolean = true
)