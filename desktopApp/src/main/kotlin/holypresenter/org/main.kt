package holypresenter.org

import androidx.compose.ui.window.application
import holypresenter.org.app.HolyPresenterApp
import holypresenter.org.platform.logging.StartupLog

fun main() {
    StartupLog.begin()

    Thread.setDefaultUncaughtExceptionHandler {
            thread,
            error ->

        StartupLog.error(
            message = "Unhandled exception in thread '${thread.name}'",
            throwable = error
        )
    }

    StartupLog.info("Entering Compose application")

    try {
        application {
            HolyPresenterApp(
                onExit = {
                    StartupLog.info("Application exit requested")
                    exitApplication()
                }
            )
        }
    } catch (error: Throwable) {
        StartupLog.error(
            message = "Fatal error in Compose application",
            throwable = error
        )

        throw error
    } finally {
        StartupLog.info("HolyPresenter process finished")
    }
}