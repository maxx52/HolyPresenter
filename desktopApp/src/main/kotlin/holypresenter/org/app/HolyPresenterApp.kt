package holypresenter.org.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import holypresenter.org.app.ui.MainWindow
import holypresenter.org.app.ui.SplashScreen
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
            onSuccess = StartupState::Ready,
            onFailure = { error ->
                StartupLog.error("Platform startup failed", error)
                StartupState.Failed(error.message ?: "Неизвестная ошибка запуска")
            }
        )
    }

    when (val currentStartup = startup) {
        StartupState.Loading,
        is StartupState.Failed -> Window(
            onCloseRequest = onExit,
            title = "HolyPresenter",
            state = rememberWindowState(
                size = DpSize(560.dp, 340.dp),
                position = WindowPosition.Aligned(Alignment.Center)
            ),
            resizable = false,
            undecorated = true
        ) {
            MaterialTheme {
                SplashScreen(
                    message = if (currentStartup is StartupState.Failed) {
                        "Ошибка запуска: ${currentStartup.message}"
                    } else {
                        "Загрузка HolyPresenter и модулей…"
                    },
                    isError = currentStartup is StartupState.Failed
                )
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

        MaterialTheme {
            MainWindow(
                modules = platform.moduleRegistry.modules,
                onDisableModule = platform::disableModule,
                onDeleteModule = platform::deleteModule,
                canDeleteModule = platform::canDeleteModule,
                disabledBuiltinModuleIds = platform.disabledBuiltinModuleIds(),
                onEnableBuiltinModule = platform::enableBuiltinModule,
                onImportModule = platform::importModuleArchive
            )
        }
    }
}
