package holypresenter.org

import androidx.compose.ui.window.application
import holypresenter.org.app.HolyPresenterApp
import holypresenter.org.app.NativeStartupSplash
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
    val splash = NativeStartupSplash.show()

    try {
        application {
            HolyPresenterApp(
                onExit = {
                    StartupLog.info("Application exit requested")
                    exitApplication()
                },
                onReady = splash::close
            )
        }
    } catch (error: Throwable) {
        StartupLog.error(
            message = "Fatal error in Compose application",
            throwable = error
        )

        throw error
    } finally {
        splash.close()
        StartupLog.info("HolyPresenter process finished")
    }
}
