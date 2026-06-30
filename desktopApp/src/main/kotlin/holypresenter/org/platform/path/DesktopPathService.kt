package holypresenter.org.platform.path

import java.io.File

class DesktopPathService : PathService {
    override val home = File(System.getProperty("user.dir"))
    override val modules = File(home, "modules")
    override val settings = File(home, "settings")
    override val layouts = File(home, "layouts")
    override val logs = File(home, "logs")

    override fun ensureDirectories() {
        modules.mkdirs()
        settings.mkdirs()
        layouts.mkdirs()
        logs.mkdirs()
    }
}