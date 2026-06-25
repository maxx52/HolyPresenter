package holypresenter.org.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import holypresenter.org.app.ui.MainWindow
import holypresenter.org.common.commands.CommandBus
import holypresenter.org.common.events.EventBus
import holypresenter.org.platform.api.module.ModuleContext
import holypresenter.org.common.module.ModuleRegistry
import holypresenter.org.common.services.ServiceRegistry
import holypresenter.org.modules.projector.ProjectorModule
import holypresenter.org.modules.welcome.WelcomeModule
import holypresenter.org.platform.core.PlatformContext
import holypresenter.org.platform.window.DefaultWindowService

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
    val commandBus = remember { CommandBus() }
    val services = remember { ServiceRegistry() }
    val windowService = remember { DefaultWindowService() }

    val platformContext = remember {
        PlatformContext(
            eventBus = eventBus,
            commandBus = commandBus,
            services = services,
            windowService = windowService
        )
    }

    val moduleRegistry = remember {
        ModuleRegistry(
            context = ModuleContext(
                platform = platformContext
            )
        ).apply {
            register(WelcomeModule())
            register(ProjectorModule())
        }
    }

    Window(
        onCloseRequest = onExit,
        title = "HolyPresenter",
        state = mainWindowState
    ) {
        MaterialTheme {
            MainWindow(
                modules = moduleRegistry.modules
            )
        }
    }
}