package holypresenter.org.platform.api.module

import androidx.compose.runtime.Composable
import holypresenter.org.platform.api.docking.DockPanel

interface HolyModule {
    val metadata: ModuleMetadata

    fun onInstall(context: ModuleContext) {}

    fun onLoad(context: ModuleContext) {}

    fun onEnable(context: ModuleContext) {}

    fun onDisable() {}

    fun onUnload() {}

    fun onUninstall() {}

    fun dockPanels(): List<DockPanel> = emptyList()

    @Composable
    fun Workspace()
}