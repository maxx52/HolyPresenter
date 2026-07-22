package holypresenter.org.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import holypresenter.org.app.ui.MainWindow
import holypresenter.org.platform.core.PlatformRuntime

@Composable
fun HolyPresenterApp(
    onExit: () -> Unit,
) {
    val platform = remember {
        PlatformRuntime()
    }

    val windowState = rememberWindowState(
        placement = WindowPlacement.Maximized
    )

    LaunchedEffect(Unit) {
        platform.start()
    }

    Window(
        onCloseRequest = {
            platform.stop()
            onExit()
        },
        title = "HolyPresenter",
        state = windowState,
    ) {
        MaterialTheme {
            MainWindow(
                modules = platform.moduleRegistry.modules
            )
        }
    }
}