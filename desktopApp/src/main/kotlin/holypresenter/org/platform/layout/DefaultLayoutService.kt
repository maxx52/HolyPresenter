package holypresenter.org.platform.layout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import holypresenter.org.platform.layout.repository.LayoutRepository

class DefaultLayoutService(
    private val repository: LayoutRepository
) : LayoutService {
    override var currentLayout by mutableStateOf(AppLayout())
        private set

    override fun load(name: String) {
        currentLayout = repository.load(name) ?: AppLayout(name = name)
    }

    override fun save() {
        repository.save(currentLayout)
    }

    override fun reset() {
        currentLayout = AppLayout()
    }

    override fun updateWindow(window: WindowLayout) {
        updateLayout { layout ->
            layout.copy(
                windows = layout.windows
                    .filterNot { it.id == window.id } + window
            )
        }
    }

    override fun updateDock(panel: DockPanelLayout) {
        updateLayout { layout ->
            layout.copy(
                dockPanels = layout.dockPanels
                    .filterNot { it.id == panel.id } + panel
            )
        }
    }

    override fun removeWindow(id: String) {
        updateLayout { layout ->
            layout.copy(
                windows = layout.windows.filterNot { it.id == id }
            )
        }
    }

    override fun removeDock(id: String) {
        updateLayout { layout ->
            layout.copy(
                dockPanels = layout.dockPanels.filterNot { it.id == id }
            )
        }
    }

    override fun window(id: String): WindowLayout? {
        return currentLayout.windows.firstOrNull { it.id == id }
    }

    override fun dock(id: String): DockPanelLayout? {
        return currentLayout.dockPanels.firstOrNull { it.id == id }
    }

    private fun updateLayout(
        transform: (AppLayout) -> AppLayout
    ) {
        currentLayout = transform(currentLayout)
    }
}