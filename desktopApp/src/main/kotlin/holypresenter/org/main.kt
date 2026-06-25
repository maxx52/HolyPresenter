package holypresenter.org

import androidx.compose.ui.window.application
import holypresenter.org.app.HolyPresenterApp

fun main() = application {
    HolyPresenterApp(
        onExit = ::exitApplication
    )
}