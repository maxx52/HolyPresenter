package holypresenter.org.platform.cloud.yandex

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Stores OAuth credentials encrypted for the current Windows user via DPAPI. */
class DpapiYandexTokenStore(
    applicationHome: File
) {
    private val directory = File(applicationHome, "cloud")
    private val tokenFile = File(directory, "yandex-token.dpapi")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun save(token: YandexOAuthToken) {
        check(isWindows()) {
            "Защищённое хранение токена Яндекс Диска пока поддерживается только в Windows"
        }
        directory.mkdirs()
        val protectedValue = invokePowerShell(
            PROTECT_SCRIPT,
            json.encodeToString(token)
        )
        tokenFile.writeText(protectedValue, Charsets.US_ASCII)
    }

    fun load(): YandexOAuthToken? {
        if (!tokenFile.isFile || !isWindows()) return null
        return runCatching {
            val plainJson = invokePowerShell(
                UNPROTECT_SCRIPT,
                tokenFile.readText(Charsets.US_ASCII).trim()
            )
            json.decodeFromString<YandexOAuthToken>(plainJson)
        }.getOrNull()
    }

    fun clear() {
        if (tokenFile.exists()) tokenFile.delete()
    }

    private fun invokePowerShell(script: String, input: String): String {
        val process = ProcessBuilder(
            "powershell.exe",
            "-NoProfile",
            "-NonInteractive",
            "-WindowStyle",
            "Hidden",
            "-ExecutionPolicy",
            "Bypass",
            "-Command",
            script
        ).redirectErrorStream(false).start()

        process.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(input)
        }
        val result = process.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val error = process.errorStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val exitCode = process.waitFor()
        check(exitCode == 0) {
            "Windows не смог защитить данные авторизации: ${error.trim().ifBlank { "код $exitCode" }}"
        }
        return result.trim()
    }

    private fun isWindows(): Boolean =
        System.getProperty("os.name").startsWith("Windows", ignoreCase = true)

    private companion object {
        val PROTECT_SCRIPT = """
            ${'$'}value = [Console]::In.ReadToEnd()
            ${'$'}bytes = [Text.Encoding]::UTF8.GetBytes(${'$'}value)
            ${'$'}protected = [Security.Cryptography.ProtectedData]::Protect(
                ${'$'}bytes,
                ${'$'}null,
                [Security.Cryptography.DataProtectionScope]::CurrentUser
            )
            [Console]::Out.Write([Convert]::ToBase64String(${'$'}protected))
        """.trimIndent()

        val UNPROTECT_SCRIPT = """
            ${'$'}value = [Console]::In.ReadToEnd().Trim()
            ${'$'}protected = [Convert]::FromBase64String(${'$'}value)
            ${'$'}bytes = [Security.Cryptography.ProtectedData]::Unprotect(
                ${'$'}protected,
                ${'$'}null,
                [Security.Cryptography.DataProtectionScope]::CurrentUser
            )
            [Console]::Out.Write([Text.Encoding]::UTF8.GetString(${'$'}bytes))
        """.trimIndent()
    }
}
