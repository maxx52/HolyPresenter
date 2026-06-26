package holypresenter.org.platform.settings.repository

import holypresenter.org.platform.settings.AppSettings

interface SettingsRepository {
    fun load(): AppSettings
    fun save(settings: AppSettings)
}