package holypresenter.org.common.dock

import holypresenter.org.common.module.HolyModule

class DockManager {
    fun collectPanels(
        modules: List<HolyModule>
    ): List<DockPanel> {
        return modules.flatMap {
            it.dockPanels()
        }
    }
}