package holypresenter.org.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import holypresenter.org.app.ui.MainWindow
import holypresenter.org.common.events.EventBus
import holypresenter.org.common.module.ModuleContext
import holypresenter.org.common.module.ModuleManager
import holypresenter.org.modules.welcome.WelcomeModule

@Composable
fun HolyPresenterApp(
    onExit: () -> Unit
) {
    val mainWindowState = rememberWindowState(
        width = 1200.dp,
        height = 800.dp,
        position = WindowPosition(80.dp, 0.dp)
    )

    val eventBus = remember { EventBus() }

    val moduleManager = remember {
        ModuleManager(
            context = ModuleContext(
                eventBus = eventBus
            )
        ).apply {
            register(WelcomeModule())
        }
    }

    Window(
        onCloseRequest = onExit,
        title = "HolyPresenter",
        state = mainWindowState
    ) {
        MaterialTheme {
            MainWindow(
                modules = moduleManager.modules
            )
        }
    }
}