package holypresenter.org.common.module

import androidx.compose.runtime.Composable
import holypresenter.org.common.dock.DockPanel

interface HolyModule {
    val id: String
    val name: String
    val version: String

    fun onLoad(context: ModuleContext) {}

    fun onUnload() {}

    fun dockPanels(): List<DockPanel> = emptyList()

    @Composable
    fun Workspace()
}