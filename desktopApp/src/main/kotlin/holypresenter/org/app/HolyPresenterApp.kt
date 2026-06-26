package holypresenter.org.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import holypresenter.org.app.ui.MainWindow
import holypresenter.org.platform.core.PlatformRuntime

@Composable
fun HolyPresenterApp(
    onExit: () -> Unit
) {
    val platform = remember {
        PlatformRuntime()
    }

    LaunchedEffect(Unit) {
        platform.start()
    }

    Window(
        onCloseRequest = {
            platform.stop()
            onExit()
        }
    ) {
        MaterialTheme {
            MainWindow(
                modules = platform.moduleRegistry.modules
            )
        }
    }
}