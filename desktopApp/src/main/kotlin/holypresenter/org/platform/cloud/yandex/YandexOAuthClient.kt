package holypresenter.org.platform.cloud.yandex

import kotlinx.serialization.json.Json
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.util.Base64

class YandexOAuthClient(
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build()
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun begin(clientId: String): PendingYandexAuthorization {
        require(clientId.isNotBlank()) { "Укажите Client ID приложения Яндекс OAuth" }
        val verifier = randomUrlSafe(64)
        val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        )
        val state = randomUrlSafe(24)
        val url = buildString {
            append(AUTHORIZE_ENDPOINT)
            append("?response_type=code")
            append("&client_id=").append(encoded(clientId))
            append("&redirect_uri=").append(encoded(REDIRECT_URI))
            append("&scope=").append(encoded(APP_FOLDER_SCOPE))
            append("&code_challenge=").append(encoded(challenge))
            append("&code_challenge_method=S256")
            append("&state=").append(encoded(state))
            append("&device_name=").append(encoded("HolyPresenter Desktop"))
            append("&device_id=").append(encoded(deviceId()))
        }
        return PendingYandexAuthorization(url, verifier, state)
    }

    fun exchangeCode(
        clientId: String,
        code: String,
        pending: PendingYandexAuthorization
    ): YandexOAuthToken = requestToken(
        mapOf(
            "grant_type" to "authorization_code",
            "code" to code.trim(),
            "client_id" to clientId,
            "redirect_uri" to REDIRECT_URI,
            "code_verifier" to pending.codeVerifier,
            "device_name" to "HolyPresenter Desktop",
            "device_id" to deviceId()
        )
    )

    fun refresh(clientId: String, refreshToken: String): YandexOAuthToken = requestToken(
        mapOf(
            "grant_type" to "refresh_token",
            "refresh_token" to refreshToken,
            "client_id" to clientId
        )
    )

    private fun requestToken(parameters: Map<String, String>): YandexOAuthToken {
        val body = parameters.entries.joinToString("&") { (name, value) ->
            "${encoded(name)}=${encoded(value)}"
        }
        val request = HttpRequest.newBuilder(URI.create(TOKEN_ENDPOINT))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() in 200..299) {
            "Яндекс не подтвердил авторизацию (${response.statusCode()}): ${safeError(response.body())}"
        }
        val decoded = json.decodeFromString<YandexOAuthToken>(response.body())
        return decoded.copy(obtainedAtEpochMillis = System.currentTimeMillis())
    }

    private fun randomUrlSafe(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun deviceId(): String {
        val source = listOf(
            System.getProperty("user.name"),
            System.getProperty("os.name"),
            System.getenv("COMPUTERNAME") ?: "desktop"
        ).joinToString("|")
        val hash = MessageDigest.getInstance("SHA-256").digest(source.toByteArray())
        return "hp-" + hash.take(12).joinToString("") { "%02x".format(it) }
    }

    private fun safeError(body: String): String = body.take(500)

    private fun encoded(value: String): String = URLEncoder.encode(value, Charsets.UTF_8)

    private companion object {
        const val AUTHORIZE_ENDPOINT = "https://oauth.yandex.com/authorize"
        const val TOKEN_ENDPOINT = "https://oauth.yandex.com/token"
        const val REDIRECT_URI = "https://oauth.yandex.com/verification_code"
        const val APP_FOLDER_SCOPE = "cloud_api:disk.app_folder"
    }
}
