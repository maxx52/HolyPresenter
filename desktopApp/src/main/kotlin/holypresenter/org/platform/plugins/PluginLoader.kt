package holypresenter.org.platform.plugins

import holypresenter.org.platform.api.module.HolyModule
import holypresenter.org.platform.logging.StartupLog
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile

class PluginLoader(
    private val bundledModulesDirectory: File,
    private val installedModulesDirectory: File
) {
    private val moduleArchives = mutableMapOf<String, File>()

    fun hasArchive(moduleId: String): Boolean = moduleArchives[moduleId]?.isFile == true

    fun deleteModuleArchive(moduleId: String): Boolean = moduleArchives.remove(moduleId)?.delete() == true

    fun importModuleArchive(source: File): File {
        require(source.isFile && source.extension.equals("jar", true)) { "Выберите JAR модуля" }
        require(declaresHolyModule(source)) { "В выбранном JAR нет модуля HolyPresenter" }
        installedModulesDirectory.mkdirs()
        val target = File(installedModulesDirectory, source.name)
        source.copyTo(target, overwrite = true)
        return target
    }
    fun loadModules(): List<HolyModule> {
        installedModulesDirectory.mkdirs()
        val directories = listOf(
            bundledModulesDirectory,
            installedModulesDirectory
        ).distinct()
        StartupLog.info(
            "[PluginLoader] modules dirs: ${directories.map(File::getAbsolutePath)}"
        )

        val jarFiles = directories.flatMap { directory ->
            directory.listFiles { file ->
                file.isFile && file.extension.equals("jar", ignoreCase = true)
            }
            ?.toList()
            ?: emptyList()
        }
            .distinctBy { file -> file.absolutePath }

        StartupLog.info("[PluginLoader] jars: ${jarFiles.map { it.name }}")

        if (jarFiles.isEmpty()) {
            return emptyList()
        }

        /*
         * Все JAR доступны каждому внешнему модулю как зависимости.
         * Но модулем считается только JAR с ServiceLoader-дескриптором.
         * Поэтому platform-ui.jar остаётся общей библиотекой и никогда
         * не может быть отключён или удалён как модуль.
         */
        val moduleJars = jarFiles.filter(::declaresHolyModule)
        val classLoader = URLClassLoader(
            jarFiles.map { it.toURI().toURL() }.toTypedArray(),
            HolyModule::class.java.classLoader
        )
        val modules = moduleJars.mapNotNull { moduleJar ->
            loadModule(moduleJar, classLoader)
        }.also { loaded ->
            loaded.forEach { module ->
                module.archiveFile()?.let { archive ->
                    moduleArchives[module.metadata.id] = archive
                }
            }
        }

        StartupLog.info(
            "[PluginLoader] loaded modules: ${modules.map { it.metadata.name }}"
        )

        return modules
    }

    /**
     * Каждый провайдер создаётся отдельно, поэтому ошибка одного JAR не
     * останавливает HolyPresenter. Общий classloader позволяет модулям видеть
     * поставляемые рядом библиотеки, например platform-ui.
     */
    private fun loadModule(
        moduleJar: File,
        classLoader: ClassLoader
    ): HolyModule? {
        val providerClassNames = providerClassNames(moduleJar)

        for (providerClassName in providerClassNames) {
            val module = runCatching {
                val providerClass = Class.forName(
                    providerClassName,
                    true,
                    classLoader
                )

                require(HolyModule::class.java.isAssignableFrom(providerClass)) {
                    "$providerClassName не реализует HolyModule"
                }

                providerClass
                    .getDeclaredConstructor()
                    .newInstance() as HolyModule
            }.onFailure { error ->
                StartupLog.error(
                    message = "[PluginLoader] failed ${moduleJar.name}, " +
                        "provider $providerClassName",
                    throwable = error
                )
            }.getOrNull()

            if (module != null) return module
        }

        StartupLog.warning(
            "[PluginLoader] skipped ${moduleJar.name}: " +
                "no loadable HolyModule provider"
        )
        return null
    }

    private fun declaresHolyModule(jar: File): Boolean =
        providerClassNames(jar).isNotEmpty()

    private fun providerClassNames(jar: File): List<String> =
        runCatching {
            JarFile(jar).use { archive ->
                val descriptor = archive.getEntry(
                    "META-INF/services/${HolyModule::class.java.name}"
                ) ?: return@use emptyList()

                archive.getInputStream(descriptor)
                    .bufferedReader()
                    .readLines()
                    .asSequence()
                    .map(String::trim)
                    .filter { it.isNotEmpty() && !it.startsWith('#') }
                    .filter { providerClass ->
                        archive.getEntry(
                            providerClass.replace('.', '/') + ".class"
                        ) != null
                    }
                    .toList()
            }
        }.onFailure { error ->
            StartupLog.error(
                message = "[PluginLoader] cannot inspect ${jar.name}",
                throwable = error
            )
        }.getOrDefault(emptyList())

    private fun HolyModule.archiveFile(): File? =
        runCatching {
            File(javaClass.protectionDomain.codeSource.location.toURI()).absoluteFile
        }.getOrNull()
}
