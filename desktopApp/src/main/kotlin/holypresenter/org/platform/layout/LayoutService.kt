package holypresenter.org.platform.layout

interface LayoutService {
    val currentLayout: AppLayout

    fun load(name: String)

    fun save()

    fun reset()

    fun updateWindow(window: WindowLayout)

    fun updateDock(panel: DockPanelLayout)

    fun removeWindow(id: String)

    fun removeDock(id: String)

    fun window(id: String): WindowLayout?

    fun dock(id: String): DockPanelLayout?
}