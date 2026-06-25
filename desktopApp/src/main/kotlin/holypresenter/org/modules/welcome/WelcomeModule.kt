package holypresenter.org.modules.welcome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import holypresenter.org.common.dock.DockPanel
import holypresenter.org.common.dock.DockPosition
import holypresenter.org.common.module.HolyModule

class WelcomeModule : HolyModule {
    override val id: String = "welcome"
    override val name: String = "Добро пожаловать"
    override val version: String = "1.0.0"

    @Composable
    override fun Workspace() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Модуль Welcome работает")
        }
    }

    override fun dockPanels(): List<DockPanel> {
        return listOf(
            DockPanel(
                id = "welcome.info",
                title = "Информация",
                position = DockPosition.RIGHT
            ) {
                Text("Панель модуля Welcome")
            }
        )
    }
}