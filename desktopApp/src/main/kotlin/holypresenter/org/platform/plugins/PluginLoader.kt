package holypresenter.org.platform.plugins

import holypresenter.org.platform.api.module.HolyModule
import java.io.File
import java.net.URLClassLoader
import java.util.ServiceLoader

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

        val modules = jarFiles.flatMap { jar ->
            val classLoader = URLClassLoader(arrayOf(jar.toURI().toURL()), HolyModule::class.java.classLoader)
            ServiceLoader.load(HolyModule::class.java, classLoader).toList().also { loaded ->
                loaded.forEach { moduleArchives[it.metadata.id] = jar }
            }
        }

        println("[PluginLoader] loaded modules: ${modules.map { it.metadata.name }}")

        return modules
    }
}
