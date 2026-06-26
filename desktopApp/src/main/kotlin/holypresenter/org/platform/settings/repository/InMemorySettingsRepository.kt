package holypresenter.org.platform.settings.repository

import holypresenter.org.platform.settings.AppSettings
import holypresenter.org.platform.settings.SettingsRepository

class InMemorySettingsRepository : SettingsRepository {
    private var settings: AppSettings? = null

    override fun load(): AppSettings? {
        return settings
    }

    override fun save(settings: AppSettings) {
        this.settings = settings
    }
}