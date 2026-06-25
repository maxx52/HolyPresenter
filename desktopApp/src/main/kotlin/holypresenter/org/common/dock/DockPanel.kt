package holypresenter.org.common.dock

data class DockPanel(
    val id: String,
    val title: String,
    val position: DockPosition,
    val content: DockContent
)