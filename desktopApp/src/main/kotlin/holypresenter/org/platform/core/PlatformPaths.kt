package holypresenter.org.platform.core

import java.io.File

object PlatformPaths {
    val home = File(System.getProperty("user.dir"))
    val modules = File(home, "modules")
    val settings = File(home, "settings")
    val layouts = File(home, "layouts")
    val logs = File(home, "logs")
}