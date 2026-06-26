package holypresenter.org.platform.layout

import kotlinx.serialization.Serializable

@Serializable
data class DockPanelLayout(
    val id: String,
    val position: String,
    val visible: Boolean = true,
    val pinned: Boolean = true
)