package holypresenter.org.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import holypresenter.org.app.ui.MainWindow
import holypresenter.org.platform.core.PlatformRuntime
import holypresenter.org.platform.logging.StartupLog

@Composable
fun HolyPresenterApp(
    onExit: () -> Unit,
) {
    val platform =
        remember {
            StartupLog.info("Creating PlatformRuntime")

            try {
                PlatformRuntime().also {
                    StartupLog.info("PlatformRuntime created successfully")
                }
            } catch (error: Throwable) {
                StartupLog.error(
                    message = "Failed to create PlatformRuntime",
                    throwable = error
                )
                throw error
            }
        }

    val windowState =
        rememberWindowState(
            placement = WindowPlacement.Maximized
        )

    LaunchedEffect(Unit) {
        StartupLog.info("Starting platform services")

        try {
            platform.start()

            StartupLog.info("Platform services started")

            StartupLog.info(
                "Registered modules: " +
                    platform.moduleRegistry.modules
                        .joinToString { module ->
                            module.metadata.name
                        }
            )
        } catch (error: Throwable) {
            StartupLog.error(
                message = "Platform startup failed",
                throwable = error
            )
            throw error
        }
    }

    Window(
        onCloseRequest = {
            StartupLog.info("Main window close requested")

            try {
                platform.stop()
                StartupLog.info("Platform stopped successfully")
            } catch (error: Throwable) {
                StartupLog.error(
                    message = "Platform shutdown failed",
                    throwable = error
                )
            } finally {
                onExit()
            }
        },
        title = "HolyPresenter",
        state = windowState,
    ) {
        LaunchedEffect(Unit) {
            StartupLog.info("Main window displayed")
        }

        MaterialTheme {
            MainWindow(
                modules = platform.moduleRegistry.modules,
                onDisableModule = platform::disableModule,
                onDeleteModule = platform::deleteModule,
                canDeleteModule = platform::canDeleteModule
            )
        }
    }
}
