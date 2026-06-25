package holypresenter.org.platform.api.docking

import androidx.compose.runtime.mutableStateListOf
import holypresenter.org.platform.api.module.HolyModule

class DockManager {
    private val _panels = mutableStateListOf<DockPanelState>()
    val panels: List<DockPanelState> = _panels

    fun registerModules(
        modules: List<HolyModule>
    ) {
        _panels.clear()

        modules
            .flatMap { it.dockPanels() }
            .forEach { panel ->
                _panels.add(
                    DockPanelState(
                        panel = panel,
                        visible = true
                    )
                )
            }
    }

    fun hide(panelId: String) {
        val index = _panels.indexOfFirst { it.panel.id == panelId }
        if (index >= 0) {
            _panels[index] = _panels[index].copy(visible = false)
        }
    }

    fun show(panelId: String) {
        val index = _panels.indexOfFirst { it.panel.id == panelId }
        if (index >= 0) {
            _panels[index] = _panels[index].copy(visible = true)
        }
    }
}