package holypresenter.org.platform.settings.repository

import holypresenter.org.platform.settings.AppSettings

class InMemorySettingsRepository : SettingsRepository {
    private var settings = AppSettings()

    override fun load(): AppSettings = settings

    override fun save(settings: AppSettings) {
        this.settings = settings
    }
}