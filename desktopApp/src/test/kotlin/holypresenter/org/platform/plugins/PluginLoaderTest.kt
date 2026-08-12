package holypresenter.org.platform.plugins

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class PluginLoaderTest {

    @Test
    fun importModuleArchive_rejectsJarWithoutHolyModuleDescriptor() {
        val directories = createDirectories()
        val archive = createJar(directories.source.resolve("ordinary.jar"))
        val loader = PluginLoader(directories.bundled, directories.installed)

        val failure = runCatching { loader.importModuleArchive(archive) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertFalse(directories.installed.resolve("ordinary.jar").exists())
    }

    @Test
    fun importModuleArchive_rejectsDescriptorWithMissingProviderClass() {
        val directories = createDirectories()
        val archive = createJar(
            directories.source.resolve("broken-module.jar"),
            serviceDescriptor = "example.missing.Module"
        )
        val loader = PluginLoader(directories.bundled, directories.installed)

        val failure = runCatching { loader.importModuleArchive(archive) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertFalse(directories.installed.resolve("broken-module.jar").exists())
    }

    @Test
    fun loadModules_skipsBrokenArchiveAndKeepsStarting() {
        val directories = createDirectories()
        createJar(
            directories.bundled.resolve("broken-module.jar"),
            serviceDescriptor = "example.missing.Module"
        )
        val loader = PluginLoader(directories.bundled, directories.installed)

        val modules = loader.loadModules()

        assertTrue(modules.isEmpty())
    }

    private fun createDirectories(): Directories {
        val root = Files.createTempDirectory("plugin-loader-test").toFile()
        return Directories(
            bundled = root.resolve("bundled").apply { mkdirs() },
            installed = root.resolve("installed").apply { mkdirs() },
            source = root.resolve("source").apply { mkdirs() }
        )
    }

    private fun createJar(
        file: java.io.File,
        serviceDescriptor: String? = null
    ): java.io.File {
        JarOutputStream(file.outputStream()).use { output ->
            serviceDescriptor?.let { content ->
                output.putNextEntry(
                    JarEntry("META-INF/services/holypresenter.org.platform.api.module.HolyModule")
                )
                output.write(content.toByteArray())
                output.closeEntry()
            }
        }
        return file
    }

    private data class Directories(
        val bundled: java.io.File,
        val installed: java.io.File,
        val source: java.io.File
    )
}
