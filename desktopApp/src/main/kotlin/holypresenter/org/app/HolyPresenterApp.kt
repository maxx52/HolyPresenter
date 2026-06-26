package holypresenter.org.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import holypresenter.org.app.ui.MainWindow
import holypresenter.org.platform.api.commands.CommandBus
import holypresenter.org.platform.api.events.EventBus
import holypresenter.org.platform.api.module.ModuleContext
import holypresenter.org.platform.core.ModuleRegistry
import holypresenter.org.platform.api.services.ServiceRegistry
import holypresenter.org.modules.projector.ProjectorModule
import holypresenter.org.modules.welcome.WelcomeModule
import holypresenter.org.platform.core.PlatformContext
import holypresenter.org.platform.layout.DefaultLayoutService
import holypresenter.org.platform.layout.repository.JsonLayoutRepository
import holypresenter.org.platform.window.DefaultWindowService
import java.io.File

@Composable
fun HolyPresenterApp(
    onExit: () -> Unit
) {
    val mainWindowState = rememberWindowState(
        width = 1200.dp,
        height = 800.dp,
        position = WindowPosition(80.dp, 0.dp)
    )

    val layoutRepository = remember {
        JsonLayoutRepository(
            File("layouts")
        )
    }

    val layoutService = remember {
        DefaultLayoutService(layoutRepository)
    }

    val windowService = remember {
        DefaultWindowService(layoutService)
    }

    layoutService.load("Default")

    val eventBus = remember { EventBus() }
    val commandBus = remember { CommandBus() }
    val services = remember { ServiceRegistry() }

    val platformContext = remember {
        PlatformContext(
            eventBus = eventBus,
            commandBus = commandBus,
            services = services,
            windowService = windowService,
            layoutService = layoutService,
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

    layoutService.save()

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