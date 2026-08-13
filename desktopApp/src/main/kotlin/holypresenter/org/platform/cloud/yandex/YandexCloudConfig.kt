package holypresenter.org.platform.cloud.yandex

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
private data class StoredYandexCloudConfig(
    val clientId: String = ""
)

class YandexCloudConfig(
    applicationHome: File
) {
    private val directory = File(applicationHome, "cloud")
    private val configFile = File(directory, "yandex.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun clientId(): String =
        System.getProperty("holypresenter.yandex.clientId")
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: System.getenv("HOLYPRESENTER_YANDEX_CLIENT_ID")
                ?.trim()
                ?.takeIf(String::isNotEmpty)
            ?: loadStored().clientId.trim().takeIf(String::isNotEmpty)
            ?: DEFAULT_CLIENT_ID

    fun saveClientId(clientId: String) {
        directory.mkdirs()
        configFile.writeText(
            json.encodeToString(StoredYandexCloudConfig(clientId.trim())),
            Charsets.UTF_8
        )
    }

    private fun loadStored(): StoredYandexCloudConfig = runCatching {
        if (!configFile.isFile) return@runCatching StoredYandexCloudConfig()
        json.decodeFromString<StoredYandexCloudConfig>(configFile.readText(Charsets.UTF_8))
    }.getOrDefault(StoredYandexCloudConfig())

    private companion object {
        // OAuth Client ID is public by design. Client secret must never be
        // embedded in a Desktop application.
        const val DEFAULT_CLIENT_ID = "d0ccacd04e2c486781826ddcd8e23a24"
    }
}
