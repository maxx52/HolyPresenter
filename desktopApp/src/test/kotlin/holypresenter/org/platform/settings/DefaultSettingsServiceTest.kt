package holypresenter.org.platform.settings

import holypresenter.org.platform.settings.repository.InMemorySettingsRepository
import junit.framework.Assert.assertEquals
import org.junit.Test

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