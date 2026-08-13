package holypresenter.org.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import holypresenter.org.app.ui.MainWindow
import holypresenter.org.app.ui.HolyTheme
import holypresenter.org.platform.core.PlatformRuntime
import holypresenter.org.platform.logging.StartupLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface StartupState {
    data object Loading : StartupState
    data class Ready(val platform: PlatformRuntime) : StartupState
    data class Failed(val message: String) : StartupState
}

@Composable
fun HolyPresenterApp(
    onExit: () -> Unit,
    onReady: () -> Unit
) {
    var startup by remember { mutableStateOf<StartupState>(StartupState.Loading) }

    LaunchedEffect(Unit) {
        startup = runCatching {
            withContext(Dispatchers.Default) {
                StartupLog.info("Creating PlatformRuntime")
                lateinit var runtime: PlatformRuntime
                runtime = PlatformRuntime(
                    onExit = {
                        runtime.stop()
                        onExit()
                    }
                )
                runtime.start()
                StartupLog.info("Platform services started")
                StartupLog.info(
                    "Registered modules: " + runtime.moduleRegistry.modules
                        .joinToString { module -> module.metadata.name }
                )
                runtime
            }
        }.fold(
            onSuccess = { platform ->
                onReady()
                StartupState.Ready(platform)
            },
            onFailure = { error ->
                StartupLog.error("Platform startup failed", error)
                onReady()
                StartupState.Failed(error.message ?: "Неизвестная ошибка запуска")
            }
        )
    }

    when (val currentStartup = startup) {
        StartupState.Loading -> Window(
            visible = false,
            onCloseRequest = onExit
        ) {}

        is StartupState.Failed -> Window(
            onCloseRequest = onExit,
            title = "HolyPresenter",
        ) {
            HolyTheme {
                androidx.compose.material3.Text("Ошибка запуска: ${currentStartup.message}")
            }
        }

        is StartupState.Ready -> MainApplicationWindow(
            platform = currentStartup.platform,
            onExit = onExit
        )
    }
}

@Composable
private fun MainApplicationWindow(
    platform: PlatformRuntime,
    onExit: () -> Unit
) {
    val windowState = rememberWindowState(placement = WindowPlacement.Maximized)

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

        HolyTheme {
            MainWindow(
                modules = platform.moduleRegistry.modules,
                onDisableModule = platform::disableModule,
                onDeleteModule = platform::deleteModule,
                canDeleteModule = platform::canDeleteModule,
                disabledBuiltinModuleIds = platform.disabledBuiltinModuleIds(),
                onEnableBuiltinModule = platform::enableBuiltinModule,
                disabledExternalModules = platform.disabledExternalModules(),
                onEnableExternalModule = platform::enableExternalModule,
                onImportModule = platform::importModuleArchive
            )
        }
    }
}
