package holypresenter.org.platform.settings

import holypresenter.org.platform.settings.repository.InMemorySettingsRepository
import org.junit.Test
import kotlin.test.assertEquals

class DefaultSettingsServiceTest {

    @Test
    fun reset_returnsDefaultSettings() {
        val repository = InMemorySettingsRepository()
        val service = DefaultSettingsService(repository)

        service.reset()

        assertEquals(AppSettings(), service.currentSettings)
    }

    @Test
    fun saveAndLoad_restoresSettings() {
        val repository = InMemorySettingsRepository()
        val service = DefaultSettingsService(repository)

        service.reset()
        service.save()
        service.load()

        assertEquals(AppSettings(), service.currentSettings)
    }
}