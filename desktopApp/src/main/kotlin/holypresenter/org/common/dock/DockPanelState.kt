package holypresenter.org.common.dock

data class DockPanelState(
    val panel: DockPanel,
    val visible: Boolean = true,
    val pinned: Boolean = true,
    val floating: Boolean = false,
    val active: Boolean = false
)