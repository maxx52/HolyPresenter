package holypresenter.org.platform.update

import holypresenter.org.platform.logging.StartupLog
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Duration

data class ApplicationUpdate(
    val version: String,
    val title: String,
    val notes: String,
    val publishedAt: String?,
    val releasePageUrl: String,
    val installer: UpdateInstallerAsset
)

data class UpdateInstallerAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val sha256: String
)

sealed interface UpdateCheckResult {
    data class Available(val update: ApplicationUpdate) : UpdateCheckResult
    data class UpToDate(val currentVersion: String) : UpdateCheckResult
}

/**
 * Downloads verified MSI packages from the official HolyPresenter GitHub release.
 *
 * The installer is never launched until its size and GitHub-provided SHA-256 digest
 * have both been verified. User data is not stored inside Program Files and is
 * therefore untouched by an in-place MSI upgrade.
 */
class ApplicationUpdateService(
    private val applicationHome: File,
    private val currentVersion: String,
    private val onExit: () -> Unit,
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(12))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build(),
    private val latestReleaseEndpoint: URI = URI.create(LATEST_RELEASE_ENDPOINT),
    private val requireSecureUrls: Boolean = true
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val updatesDirectory = File(applicationHome, "updates")

    fun checkForUpdates(): UpdateCheckResult {
        val request = HttpRequest.newBuilder(latestReleaseEndpoint)
            .timeout(Duration.ofSeconds(25))
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2026-03-10")
            .header("User-Agent", "HolyPresenter/$currentVersion")
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(Charsets.UTF_8))
        check(response.statusCode() in 200..299) {
            when (response.statusCode()) {
                404 -> "Для HolyPresenter пока нет опубликованного обновления."
                403, 429 -> "GitHub временно ограничил проверку обновлений. Повторите позже."
                else -> "Не удалось проверить обновления: HTTP ${response.statusCode()}."
            }
        }

        val update = parseLatestRelease(response.body())
        return if (VersionNumber.parse(update.version) > VersionNumber.parse(currentVersion)) {
            UpdateCheckResult.Available(update)
        } else {
            UpdateCheckResult.UpToDate(currentVersion)
        }
    }

    fun download(
        update: ApplicationUpdate,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): File {
        val asset = update.installer
        validateAsset(asset)
        updatesDirectory.mkdirs()
        check(updatesDirectory.isDirectory) {
            "Не удалось создать каталог обновлений: ${updatesDirectory.absolutePath}"
        }
        val usableSpace = Files.getFileStore(updatesDirectory.toPath()).usableSpace
        check(usableSpace >= asset.sizeBytes + MIN_FREE_SPACE_BYTES) {
            "Недостаточно места для обновления. Освободите хотя бы ${humanSize(asset.sizeBytes + MIN_FREE_SPACE_BYTES)}."
        }

        val finalFile = File(updatesDirectory, "HolyPresenter-${safeVersion(update.version)}.msi")
        if (finalFile.isFile && verifyFile(finalFile, asset)) {
            onProgress(asset.sizeBytes, asset.sizeBytes)
            return finalFile
        }

        val partialFile = File(updatesDirectory, finalFile.name + ".part")
        partialFile.delete()
        val request = HttpRequest.newBuilder(URI.create(asset.downloadUrl))
            .timeout(Duration.ofMinutes(15))
            .header("Accept", "application/octet-stream")
            .header("User-Agent", "HolyPresenter/$currentVersion")
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
        check(response.statusCode() in 200..299) {
            "Не удалось скачать обновление: HTTP ${response.statusCode()}."
        }

        val digest = MessageDigest.getInstance("SHA-256")
        var downloaded = 0L
        try {
            response.body().use { input ->
                partialFile.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        downloaded += count
                        check(downloaded <= MAX_INSTALLER_SIZE_BYTES) {
                            "Загружаемый установщик превышает допустимый размер."
                        }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                        onProgress(downloaded, asset.sizeBytes)
                    }
                }
            }
            check(downloaded == asset.sizeBytes) {
                "Обновление загружено не полностью: ${humanSize(downloaded)} из ${humanSize(asset.sizeBytes)}."
            }
            val actualDigest = digest.digest().toHex()
            check(actualDigest.equals(asset.sha256, ignoreCase = true)) {
                "Контрольная сумма обновления не совпала. Файл удалён для безопасности."
            }
            try {
                Files.move(
                    partialFile.toPath(),
                    finalFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(
                    partialFile.toPath(),
                    finalFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        } catch (error: Throwable) {
            partialFile.delete()
            throw error
        }
        return finalFile
    }

    /** Starts a hidden helper, closes HolyPresenter, installs the MSI and reopens the app. */
    fun installAfterExit(update: ApplicationUpdate, installerFile: File): Result<Unit> = runCatching {
        check(System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
            "Автоматическая установка сейчас поддерживается только в Windows."
        }
        check(installerFile.isFile && installerFile.extension.equals("msi", ignoreCase = true)) {
            "Файл обновления не найден. Скачайте его ещё раз."
        }
        check(verifyFile(installerFile, update.installer)) {
            "Проверка файла обновления не пройдена. Скачайте его ещё раз."
        }

        updatesDirectory.mkdirs()
        val script = File(updatesDirectory, "install-${safeVersion(update.version)}.ps1")
        val applicationPath = packagedApplicationPath()
        val scriptText = buildInstallScript(
            processId = ProcessHandle.current().pid(),
            installerPath = installerFile.absolutePath,
            applicationPath = applicationPath
        )
        writeUtf8Bom(script, scriptText)

        ProcessBuilder(
            "powershell.exe",
            "-NoProfile",
            "-NonInteractive",
            "-ExecutionPolicy",
            "Bypass",
            "-WindowStyle",
            "Hidden",
            "-File",
            script.absolutePath
        )
            .directory(updatesDirectory)
            .start()

        StartupLog.info("Update ${update.version} scheduled from ${installerFile.absolutePath}")
        onExit()
    }

    internal fun parseLatestRelease(payload: String): ApplicationUpdate {
        val root = json.parseToJsonElement(payload).jsonObject
        check(root["draft"]?.jsonPrimitive?.booleanOrNull != true) {
            "GitHub вернул черновик выпуска вместо опубликованной версии."
        }
        check(root["prerelease"]?.jsonPrimitive?.booleanOrNull != true) {
            "Предварительные версии не устанавливаются автоматически."
        }
        val tag = root.string("tag_name") ?: error("В выпуске не указана версия.")
        val version = VersionNumber.parse(tag).display
        val assets = root["assets"]?.jsonArray ?: error("В выпуске нет файлов.")
        val installerObject = assets
            .map { it.jsonObject }
            .filter { it.string("name")?.endsWith(".msi", ignoreCase = true) == true }
            .sortedByDescending { it.string("name")?.contains("HolyPresenter", ignoreCase = true) == true }
            .firstOrNull()
            ?: error("В выпуске нет MSI-установщика HolyPresenter.")
        val digest = installerObject.string("digest")
            ?.takeIf { it.startsWith("sha256:", ignoreCase = true) }
            ?.substringAfter(':')
            ?.lowercase()
            ?: error("У MSI-установщика нет SHA-256. Обновление заблокировано для безопасности.")
        check(SHA256_REGEX.matches(digest)) { "GitHub вернул неверную SHA-256 установщика." }

        return ApplicationUpdate(
            version = version,
            title = root.string("name")?.takeIf(String::isNotBlank) ?: "HolyPresenter $version",
            notes = root.string("body").orEmpty().trim(),
            publishedAt = root.string("published_at"),
            releasePageUrl = root.string("html_url") ?: error("В выпуске нет ссылки на страницу."),
            installer = UpdateInstallerAsset(
                name = installerObject.string("name") ?: error("У установщика нет имени."),
                downloadUrl = installerObject.string("browser_download_url")
                    ?: error("У установщика нет ссылки для скачивания."),
                sizeBytes = installerObject["size"]?.jsonPrimitive?.longOrNull
                    ?: error("У установщика не указан размер."),
                sha256 = digest
            )
        ).also { validateAsset(it.installer) }
    }

    internal fun buildInstallScript(
        processId: Long,
        installerPath: String,
        applicationPath: String?
    ): String = buildString {
        appendLine("\$ErrorActionPreference = 'Stop'")
        appendLine("Wait-Process -Id $processId -ErrorAction SilentlyContinue")
        appendLine("\$installer = '${powershellLiteral(installerPath)}'")
        appendLine("\$arguments = @('/i', ('\"' + \$installer + '\"'), '/passive', '/norestart')")
        appendLine("try {")
        appendLine("    \$result = Start-Process -FilePath 'msiexec.exe' -ArgumentList \$arguments -Verb RunAs -Wait -PassThru")
        appendLine("    if (\$result.ExitCode -notin @(0, 1641, 3010)) { throw \"MSI exit code: \$(\$result.ExitCode)\" }")
        appendLine("} catch {")
        appendLine("    \$_ | Out-String | Set-Content -LiteralPath (Join-Path (Split-Path \$installer) 'last-update-error.txt') -Encoding UTF8")
        appendLine("} finally {")
        if (applicationPath != null) {
            appendLine("    \$application = '${powershellLiteral(applicationPath)}'")
            appendLine("    if (Test-Path -LiteralPath \$application) { Start-Process -FilePath \$application }")
        }
        appendLine("    Remove-Item -LiteralPath \$MyInvocation.MyCommand.Path -Force -ErrorAction SilentlyContinue")
        appendLine("}")
    }

    private fun validateAsset(asset: UpdateInstallerAsset) {
        val uri = URI.create(asset.downloadUrl)
        if (requireSecureUrls) {
            check(uri.scheme.equals("https", ignoreCase = true) && uri.host.equals("github.com", ignoreCase = true)) {
                "Обновление разрешено скачивать только с официального GitHub."
            }
        }
        check(asset.name.endsWith(".msi", ignoreCase = true)) { "Файл обновления должен быть MSI." }
        check(asset.name.contains("HolyPresenter", ignoreCase = true)) {
            "В выпуске выбран MSI, который не относится к HolyPresenter."
        }
        check(asset.sizeBytes in 1..MAX_INSTALLER_SIZE_BYTES) { "Неверный размер файла обновления." }
        check(SHA256_REGEX.matches(asset.sha256)) { "Неверная контрольная сумма файла обновления." }
    }

    private fun verifyFile(file: File, asset: UpdateInstallerAsset): Boolean {
        if (!file.isFile || file.length() != asset.sizeBytes) return false
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().toHex().equals(asset.sha256, ignoreCase = true)
    }

    private fun packagedApplicationPath(): String? = sequenceOf(
        System.getProperty("jpackage.app-path"),
        File(System.getProperty("user.dir"), "HolyPresenter.exe").absolutePath
    ).filterNotNull().map(::File).firstOrNull(File::isFile)?.absolutePath

    private fun writeUtf8Bom(file: File, text: String) {
        file.outputStream().use { output ->
            output.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            output.write(text.toByteArray(Charsets.UTF_8))
        }
    }

    private fun powershellLiteral(value: String): String = value.replace("'", "''")
    private fun safeVersion(value: String): String = value.replace(Regex("[^0-9A-Za-z._-]"), "-")

    private fun humanSize(bytes: Long): String = when {
        bytes >= 1024L * 1024L * 1024L -> "%.1f ГБ".format(bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024L * 1024L -> "%.1f МБ".format(bytes / (1024.0 * 1024.0))
        else -> "%.1f КБ".format(bytes / 1024.0)
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    private fun kotlinx.serialization.json.JsonObject.string(name: String): String? =
        get(name)?.jsonPrimitive?.contentOrNull

    private companion object {
        const val LATEST_RELEASE_ENDPOINT =
            "https://api.github.com/repos/maxx52/HolyPresenter/releases/latest"
        const val MAX_INSTALLER_SIZE_BYTES = 2L * 1024L * 1024L * 1024L
        const val MIN_FREE_SPACE_BYTES = 64L * 1024L * 1024L
        val SHA256_REGEX = Regex("[0-9a-fA-F]{64}")
    }
}

internal data class VersionNumber(
    val numbers: List<Int>,
    val qualifier: String?,
    val display: String
) : Comparable<VersionNumber> {
    override fun compareTo(other: VersionNumber): Int {
        val length = maxOf(numbers.size, other.numbers.size)
        repeat(length) { index ->
            val comparison = (numbers.getOrNull(index) ?: 0)
                .compareTo(other.numbers.getOrNull(index) ?: 0)
            if (comparison != 0) return comparison
        }
        if (qualifier == null && other.qualifier != null) return 1
        if (qualifier != null && other.qualifier == null) return -1
        return qualifier.orEmpty().compareTo(other.qualifier.orEmpty(), ignoreCase = true)
    }

    companion object {
        fun parse(raw: String): VersionNumber {
            val normalized = raw.trim().removePrefix("v").removePrefix("V")
            val main = normalized.substringBefore('-')
            val qualifier = normalized.substringAfter('-', "").ifBlank { null }
            val numbers = main.split('.').map { part ->
                part.toIntOrNull() ?: error("Неверный номер версии: $raw")
            }
            check(numbers.isNotEmpty() && numbers.size <= 4) { "Неверный номер версии: $raw" }
            return VersionNumber(numbers, qualifier, main + qualifier?.let { "-$it" }.orEmpty())
        }
    }
}
