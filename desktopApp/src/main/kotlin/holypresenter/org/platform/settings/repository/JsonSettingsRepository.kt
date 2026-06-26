package holypresenter.org.platform.settings.repository

import holypresenter.org.platform.settings.AppSettings
import kotlinx.serialization.json.Json
import java.io.File

class JsonSettingsRepository(
    private val settingsFile: File
) : SettingsRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    init {
        settingsFile.parentFile?.mkdirs()
    }

    override fun load(): AppSettings {
        if (!settingsFile.exists()) {
            return AppSettings()
        }

        return json.decodeFromString(settingsFile.readText())
    }

    override fun save(settings: AppSettings) {
        settingsFile.writeText(
            json.encodeToString(settings)
        )
    }
}