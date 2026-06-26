package holypresenter.org.platform.api.docking

data class DockPanel(
    val id: String,
    val title: String,
    val position: DockPosition,
    val content: DockContent
)