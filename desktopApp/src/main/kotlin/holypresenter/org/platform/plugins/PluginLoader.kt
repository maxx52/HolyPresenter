package holypresenter.org.platform.plugins

import holypresenter.org.platform.api.module.HolyModule
import java.io.File
import java.net.URLClassLoader
import java.util.ServiceLoader

class PluginLoader(
    private val modulesDirectory: File
) {

    fun loadModules(): List<HolyModule> {
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

        if (jarFiles.isEmpty()) {
            return emptyList()
        }

        val urls = jarFiles
            .map { it.toURI().toURL() }
            .toTypedArray()

        val classLoader = URLClassLoader(
            urls,
            HolyModule::class.java.classLoader
        )

        return ServiceLoader
            .load(HolyModule::class.java, classLoader)
            .toList()
    }
}