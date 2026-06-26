package holypresenter.org.platform.settings

import kotlinx.serialization.Serializable

@Serializable
data class WindowSettings(
    val rememberPositions: Boolean = true,
    val restoreOnStartup: Boolean = true
)