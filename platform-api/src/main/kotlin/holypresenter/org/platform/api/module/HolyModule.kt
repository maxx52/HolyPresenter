package holypresenter.org.platform.api.module

/**
 * Base interface for every HolyPresenter module.
 *
 * Modules are loaded by the runtime and receive
 * ModuleContext during initialization.
 */

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