package holypresenter.org.modules.welcome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import holypresenter.org.platform.api.module.HolyModule
import holypresenter.org.platform.api.module.ModuleMetadata

class WelcomeModule : HolyModule {

    override val metadata = ModuleMetadata(
        id = "welcome",
        name = "Добро пожаловать",
        version = "1.0.0",
        author = "HolyPresenter",
        description = "Тестовый модуль приветствия",
        apiVersion = "1.0.0",
    )

    @Composable
    override fun Workspace() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Модуль Welcome работает")
        }
    }
}