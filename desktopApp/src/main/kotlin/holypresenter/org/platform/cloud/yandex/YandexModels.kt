package holypresenter.org.platform.cloud.yandex

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YandexOAuthToken(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresInSeconds: Long? = null,
    @SerialName("token_type") val tokenType: String = "bearer",
    val scope: String? = null,
    val obtainedAtEpochMillis: Long = System.currentTimeMillis()
) {
    fun isExpiring(now: Long = System.currentTimeMillis()): Boolean {
        val lifetime = expiresInSeconds ?: return false
        return now >= obtainedAtEpochMillis + lifetime * 1_000L - 60_000L
    }
}

data class PendingYandexAuthorization(
    val authorizationUrl: String,
    internal val codeVerifier: String,
    internal val state: String
)

data class YandexRemoteBackup(
    val name: String,
    val path: String,
    val size: Long,
    val modified: String?
)

@Serializable
internal data class YandexLinkResponse(
    val href: String,
    val method: String = "GET",
    val templated: Boolean = false
)

@Serializable
internal data class YandexResourceListResponse(
    @SerialName("_embedded") val embedded: YandexEmbeddedResources? = null
)

@Serializable
internal data class YandexEmbeddedResources(
    val items: List<YandexResource> = emptyList()
)

@Serializable
internal data class YandexResource(
    val name: String,
    val path: String,
    val type: String,
    val size: Long = 0,
    val modified: String? = null
)

