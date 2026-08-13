package holypresenter.org.platform.cloud.yandex

import com.sun.jna.platform.win32.Crypt32Util
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Base64

internal interface TokenProtector {
    fun protect(value: ByteArray): ByteArray
    fun unprotect(value: ByteArray): ByteArray
}

private object WindowsDpapiTokenProtector : TokenProtector {
    override fun protect(value: ByteArray): ByteArray =
        Crypt32Util.cryptProtectData(value)

    override fun unprotect(value: ByteArray): ByteArray =
        Crypt32Util.cryptUnprotectData(value)
}

/** Stores OAuth credentials encrypted for the current Windows user via DPAPI. */
class DpapiYandexTokenStore private constructor(
    applicationHome: File,
    private val protector: TokenProtector,
    private val windowsHost: () -> Boolean
) {
    constructor(applicationHome: File) : this(
        applicationHome = applicationHome,
        protector = WindowsDpapiTokenProtector,
        windowsHost = ::isWindowsHost
    )

    internal constructor(
        applicationHome: File,
        protector: TokenProtector
    ) : this(
        applicationHome = applicationHome,
        protector = protector,
        windowsHost = { true }
    )

    private val directory = File(applicationHome, "cloud")
    private val tokenFile = File(directory, "yandex-token.dpapi")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun save(token: YandexOAuthToken) {
        check(windowsHost()) {
            "Защищённое хранение токена Яндекс Диска пока поддерживается только в Windows"
        }
        directory.mkdirs()
        val plainBytes = json.encodeToString(token).toByteArray(Charsets.UTF_8)
        try {
            val protectedValue = Base64.getEncoder().encodeToString(
                protector.protect(plainBytes)
            )
            tokenFile.writeText(protectedValue, Charsets.US_ASCII)
        } finally {
            plainBytes.fill(0)
        }
    }

    fun load(): YandexOAuthToken? {
        if (!tokenFile.isFile || !windowsHost()) return null
        return runCatching {
            val protectedBytes = Base64.getDecoder().decode(
                tokenFile.readText(Charsets.US_ASCII).trim()
            )
            val plainBytes = protector.unprotect(protectedBytes)
            try {
                json.decodeFromString<YandexOAuthToken>(
                    plainBytes.toString(Charsets.UTF_8)
                )
            } finally {
                plainBytes.fill(0)
                protectedBytes.fill(0)
            }
        }.getOrNull()
    }

    fun clear() {
        if (tokenFile.exists()) tokenFile.delete()
    }

    private companion object {
        fun isWindowsHost(): Boolean =
            System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
    }
}
