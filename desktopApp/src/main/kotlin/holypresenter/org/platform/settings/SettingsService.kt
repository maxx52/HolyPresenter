package holypresenter.org.platform.settings

interface SettingsService {
    val currentSettings: AppSettings

    fun load()

    fun save()

    fun reset()
}