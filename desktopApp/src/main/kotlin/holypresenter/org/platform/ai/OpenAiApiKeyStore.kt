package holypresenter.org.platform.ai

import com.sun.jna.platform.win32.Crypt32Util
import java.io.File
import java.util.Base64

internal interface AiSecretProtector {
    fun protect(value: ByteArray): ByteArray
    fun unprotect(value: ByteArray): ByteArray
}

private object WindowsAiSecretProtector : AiSecretProtector {
    override fun protect(value: ByteArray): ByteArray =
        Crypt32Util.cryptProtectData(value)

    override fun unprotect(value: ByteArray): ByteArray =
        Crypt32Util.cryptUnprotectData(value)
}

/** The key is encrypted for the current Windows user and excluded from cloud backups. */
class OpenAiApiKeyStore private constructor(
    applicationHome: File,
    private val protector: AiSecretProtector,
    private val windowsHost: () -> Boolean,
    private val environment: () -> String?
) {
    constructor(applicationHome: File) : this(
        applicationHome = applicationHome,
        protector = WindowsAiSecretProtector,
        windowsHost = ::isWindows,
        environment = { System.getenv("OPENAI_API_KEY") }
    )

    internal constructor(
        applicationHome: File,
        protector: AiSecretProtector,
        environment: () -> String? = { null }
    ) : this(
        applicationHome = applicationHome,
        protector = protector,
        windowsHost = { true },
        environment = environment
    )

    private val secretDirectory = File(applicationHome, "cloud")
    private val secretFile = File(secretDirectory, "openai-api-key.dpapi")

    fun isConfigured(): Boolean = load() != null

    fun save(apiKey: String) {
        val normalized = apiKey.trim()
        require(normalized.startsWith("sk-") && normalized.length >= 20) {
            "Ключ OpenAI должен начинаться с sk-"
        }
        check(windowsHost()) {
            "Защищённое хранение ключа OpenAI поддерживается только в Windows"
        }
        secretDirectory.mkdirs()
        val plainBytes = normalized.toByteArray(Charsets.UTF_8)
        try {
            val encrypted = protector.protect(plainBytes)
            try {
                secretFile.writeText(
                    Base64.getEncoder().encodeToString(encrypted),
                    Charsets.US_ASCII
                )
            } finally {
                encrypted.fill(0)
            }
        } finally {
            plainBytes.fill(0)
        }
    }

    fun load(): String? {
        environment()?.trim()?.takeIf(String::isNotBlank)?.let { return it }
        if (!windowsHost() || !secretFile.isFile) return null
        return runCatching {
            val encrypted = Base64.getDecoder().decode(
                secretFile.readText(Charsets.US_ASCII).trim()
            )
            val plainBytes = protector.unprotect(encrypted)
            try {
                plainBytes.toString(Charsets.UTF_8).trim().takeIf(String::isNotBlank)
            } finally {
                plainBytes.fill(0)
                encrypted.fill(0)
            }
        }.getOrNull()
    }

    fun clear() {
        if (secretFile.exists()) secretFile.delete()
    }

    fun usesEnvironmentKey(): Boolean =
        !environment().isNullOrBlank()

    private companion object {
        fun isWindows(): Boolean =
            System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
    }
}
