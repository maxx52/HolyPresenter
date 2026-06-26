package holypresenter.org.platform.settings

interface SettingsRepository {
    fun load(): AppSettings?
    fun save(settings: AppSettings)
}