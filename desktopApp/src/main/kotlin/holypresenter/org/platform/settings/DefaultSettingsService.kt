package holypresenter.org.platform.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import holypresenter.org.platform.settings.repository.SettingsRepository

class DefaultSettingsService(
    private val repository: SettingsRepository
) : SettingsService {

    override var currentSettings: AppSettings by mutableStateOf(repository.load())
        private set

    override fun load() {
        currentSettings = repository.load()
    }

    override fun save() {
        repository.save(currentSettings)
    }

    override fun reset() {
        currentSettings = AppSettings()
    }
}