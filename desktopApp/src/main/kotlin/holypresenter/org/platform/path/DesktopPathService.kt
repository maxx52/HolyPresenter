package holypresenter.org.platform.path

import java.io.File

class DesktopPathService : PathService {
    /*
     * Рабочая папка используется для режима разработки.
     */
    private val workingDirectory: File =
        File(
            System.getProperty("user.dir")
        ).absoluteFile

    /*
     * В упакованном приложении Compose передаёт
     * путь к дополнительным ресурсам через это свойство.
     */
    private val packagedResourcesDirectory: File? =
        System.getProperty(
            "compose.application.resources.dir"
        )
            ?.takeIf { path ->
                path.isNotBlank()
            }
            ?.let { path ->
                File(path).absoluteFile
            }

    override val home: File = workingDirectory

    /*
     * В установленной программе берём модули
     * из ресурсов дистрибутива.
     *
     * При запуске из IDE ищем:
     *
     * 1. <user.dir>/modules
     * 2. <user.dir>/desktopApp/modules
     */
    override val modules: File =
        packagedResourcesDirectory
            ?.resolve("modules")
            ?: resolveDevelopmentModulesDirectory()

    override val settings: File = File(home, "settings")
    override val layouts: File = File(home, "layouts")
    override val logs: File = File(home, "logs")

    override fun ensureDirectories() {
        /*
         * Папка ресурсов установленной программы
         * может быть доступна только для чтения.
         */
        if (packagedResourcesDirectory == null) {
            modules.mkdirs()
        }
        settings.mkdirs()
        layouts.mkdirs()
        logs.mkdirs()
    }

    private fun resolveDevelopmentModulesDirectory(): File {
        val directModules = File(workingDirectory, "modules")
        val modulesFromProjectRoot = File(workingDirectory, "desktopApp/modules")

        return when {
            directModules.isDirectory -> directModules
            modulesFromProjectRoot.isDirectory -> modulesFromProjectRoot
            else -> directModules
        }
    }
}