package holypresenter.org.platform.settings

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val platform: PlatformSettings = PlatformSettings(),
    val window: WindowSettings = WindowSettings()
)