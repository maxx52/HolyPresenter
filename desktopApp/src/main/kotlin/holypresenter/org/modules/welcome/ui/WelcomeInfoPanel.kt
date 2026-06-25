package holypresenter.org.modules.welcome.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import holypresenter.org.platform.api.docking.DockContent

class WelcomeInfoPanel : DockContent {
    @Composable
    override fun Content() {
        Text("Панель модуля Welcome")
    }
}