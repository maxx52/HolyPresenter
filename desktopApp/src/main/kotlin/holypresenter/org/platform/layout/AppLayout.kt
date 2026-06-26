package holypresenter.org.platform.layout

import kotlinx.serialization.Serializable

@Serializable
data class AppLayout(
    val name: String = "Default",
    val windows: List<WindowLayout> = emptyList(),
    val dockPanels: List<DockPanelLayout> = emptyList()
)