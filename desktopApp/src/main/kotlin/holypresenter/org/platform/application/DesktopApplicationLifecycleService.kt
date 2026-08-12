package holypresenter.org.platform.application

import holypresenter.org.platform.api.application.ApplicationLifecycleService
import java.io.File

class DesktopApplicationLifecycleService(
    private val onExit: () -> Unit
) : ApplicationLifecycleService {
    override fun restart(): Result<Unit> = runCatching {
        val process = ProcessHandle.current().info()
        val command = process.command().orElseThrow {
            IllegalStateException("Не удалось определить команду запуска HolyPresenter")
        }
        val arguments = process.arguments().orElse(emptyArray())
        ProcessBuilder(listOf(command) + arguments)
            .directory(File(System.getProperty("user.dir")))
            .start()
        onExit()
    }
}
