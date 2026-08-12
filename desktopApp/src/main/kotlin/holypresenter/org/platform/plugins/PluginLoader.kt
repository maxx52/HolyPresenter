package holypresenter.org.platform.plugins

import holypresenter.org.platform.api.module.HolyModule
import java.io.File
import java.net.URLClassLoader
import java.util.ServiceLoader
import java.util.jar.JarFile

class PluginLoader(
    private val modulesDirectory: File
) {
    private val moduleArchives = mutableMapOf<String, File>()

    fun hasArchive(moduleId: String): Boolean = moduleArchives[moduleId]?.isFile == true

    fun deleteModuleArchive(moduleId: String): Boolean = moduleArchives.remove(moduleId)?.delete() == true
    fun loadModules(): List<HolyModule> {
        println("[PluginLoader] modules dir: ${modulesDirectory.absolutePath}")
        println("[PluginLoader] exists: ${modulesDirectory.exists()}")

        if (!modulesDirectory.exists()) {
            modulesDirectory.mkdirs()
            return emptyList()
        }

        val jarFiles = modulesDirectory
            .listFiles { file ->
                file.isFile && file.extension.equals("jar", ignoreCase = true)
            }
            ?.toList()
            ?: emptyList()

        println("[PluginLoader] jars: ${jarFiles.map { it.name }}")

        if (jarFiles.isEmpty()) {
            return emptyList()
        }

        /*
         * Все JAR доступны каждому внешнему модулю как зависимости.
         * Но модулем считается только JAR с ServiceLoader-дескриптором.
         * Поэтому platform-ui.jar остаётся общей библиотекой и никогда
         * не может быть отключён или удалён как модуль.
         */
        val classLoader = URLClassLoader(
            jarFiles.map { it.toURI().toURL() }.toTypedArray(),
            HolyModule::class.java.classLoader
        )
        val moduleJars = jarFiles.filter(::declaresHolyModule)
        val modules =
            ServiceLoader.load(HolyModule::class.java, classLoader)
                .toList()
                .filter { module ->
                    module.archiveFile() in moduleJars
                }
                .also { loaded ->
                    loaded.forEach { module ->
                        module.archiveFile()?.let { archive ->
                            moduleArchives[module.metadata.id] = archive
                        }
                    }
                }

        println("[PluginLoader] loaded modules: ${modules.map { it.metadata.name }}")

        return modules
    }

    private fun declaresHolyModule(jar: File): Boolean =
        runCatching {
            JarFile(jar).use { archive ->
                archive.getEntry("META-INF/services/${HolyModule::class.java.name}") != null
            }
        }.getOrDefault(false)

    private fun HolyModule.archiveFile(): File? =
        runCatching {
            File(javaClass.protectionDomain.codeSource.location.toURI()).absoluteFile
        }.getOrNull()
}
