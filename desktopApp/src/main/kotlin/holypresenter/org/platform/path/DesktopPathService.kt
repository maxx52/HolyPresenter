package holypresenter.org.platform.path

import java.io.File

class DesktopPathService : PathService {
    private val workingDirectory: File =
        File(
            System.getProperty("user.dir")
        ).absoluteFile

    /*
     * Дополнительные ресурсы установленной программы.
     * Здесь находятся встроенные модули.
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

    /*
     * Все изменяемые пользовательские данные
     * должны находиться вне Program Files.
     */
    override val home: File = resolveApplicationDataDirectory()

    /*
     * Модули установленной программы читаем
     * из ресурсов дистрибутива.
     */
    override val modules: File =
        packagedResourcesDirectory
            ?.resolve("modules")
            ?: resolveDevelopmentModulesDirectory()

    override val settings: File = File(home, "settings")
    override val layouts: File = File(home, "layouts")
    override val logs: File = File(home, "logs")

    override fun ensureDirectories() {
        ensureWritableDirectory(
            directory = home,
            description = "каталог данных HolyPresenter"
        )

        ensureWritableDirectory(
            directory = settings,
            description = "каталог настроек"
        )

        ensureWritableDirectory(
            directory = layouts,
            description = "каталог раскладок"
        )

        ensureWritableDirectory(
            directory = logs,
            description = "каталог журналов"
        )

        /*
         * В установленной версии modules находится
         * внутри ресурсов и не должна изменяться.
         */
        if (packagedResourcesDirectory == null) {
            if (!modules.exists()) {
                modules.mkdirs()
            }
        }
    }

    private fun resolveApplicationDataDirectory(): File {
        val localAppData =
            System.getenv("LOCALAPPDATA")
                ?.takeIf { path ->
                    path.isNotBlank()
                }
                ?.let(::File)

        return if (localAppData != null) {
            File(
                localAppData,
                "HolyPresenter"
            )
        } else {
            File(
                System.getProperty("user.home"),
                ".holypresenter"
            )
        }.absoluteFile
    }

    private fun resolveDevelopmentModulesDirectory(): File {
        val directModules =
            File(
                workingDirectory,
                "modules"
            )

        val projectModules =
            File(
                workingDirectory,
                "desktopApp/modules"
            )

        return when {
            directModules.isDirectory -> directModules
            projectModules.isDirectory -> projectModules
            else -> directModules
        }
    }

    private fun ensureWritableDirectory(
        directory: File,
        description: String
    ) {
        if (
            !directory.exists() &&
            !directory.mkdirs()
        ) {
            error(
                "Не удалось создать $description: " +
                        directory.absolutePath
            )
        }

        require(directory.isDirectory) {
            "$description не является каталогом: " +
                    directory.absolutePath
        }

        require(directory.canWrite()) {
            "Нет доступа на запись в $description: " +
                    directory.absolutePath
        }
    }
}