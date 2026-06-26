package holypresenter.org.platform.settings

import kotlinx.serialization.Serializable

@Serializable
data class PlatformSettings(
    val language: String = "ru",
    val theme: String = "system"
)