package holypresenter.org.platform.settings

import holypresenter.org.platform.settings.repository.JsonSettingsRepository
import org.junit.Test
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonSettingsRepositoryTest {
    @Test
    fun load_returnsDefaultSettingsWhenFileDoesNotExist() {
        val tempDir = Files.createTempDirectory("settings-test").toFile()
        val repository = JsonSettingsRepository(tempDir.resolve("platform.json"))
        val settings = repository.load()

        assertEquals(AppSettings(), settings)
    }

    @Test
    fun save_createsSettingsFile() {
        val tempDir = Files.createTempDirectory("settings-test").toFile()
        val file = tempDir.resolve("platform.json")
        val repository = JsonSettingsRepository(file)

        repository.save(AppSettings())

        assertTrue(file.exists())
    }

    @Test
    fun saveAndLoad_returnsSavedSettings() {
        val tempDir = Files.createTempDirectory("settings-test").toFile()
        val repository = JsonSettingsRepository(tempDir.resolve("platform.json"))

        val settings = AppSettings(
            platform = PlatformSettings(
                language = "en",
                theme = "dark"
            )
        )

        repository.save(settings)

        assertEquals(settings, repository.load())
    }
}