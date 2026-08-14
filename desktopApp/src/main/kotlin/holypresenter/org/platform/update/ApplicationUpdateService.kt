package holypresenter.org.platform.update

import holypresenter.org.platform.logging.StartupLog
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.net.URI
import java.net.URLEncoder
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
    val sha256: String,
    val source: UpdateDistributionSource = UpdateDistributionSource.GITHUB
)

enum class UpdateDistributionSource {
    YANDEX_DISK,
    GITHUB
}

sealed interface UpdateCheckResult {
    data class Available(val update: ApplicationUpdate) : UpdateCheckResult
    data class UpToDate(val currentVersion: String) : UpdateCheckResult
}

/**
 * Downloads verified MSI packages from the HolyPresenter update mirrors.
 *
 * The installer is never launched until its size and published SHA-256 digest
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
    private val yandexPublicFolderUrl: String? = YANDEX_PUBLIC_FOLDER_URL,
    private val yandexPublicDownloadEndpoint: URI = URI.create(YANDEX_PUBLIC_DOWNLOAD_ENDPOINT),
    private val requireSecureUrls: Boolean = true,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val updatesDirectory = File(applicationHome, "updates")
    private val cachedManifestFile = File(updatesDirectory, "latest-update.json")
    private val cachedSourceFile = File(updatesDirectory, "latest-update-source.txt")
    private val lastCheckFile = File(updatesDirectory, "last-check.txt")

    fun checkForUpdates(forceRefresh: Boolean = false): UpdateCheckResult {
        if (!forceRefresh && cacheIsFresh()) {
            return cachedResult()
        }

        val yandexResult = runCatching {
            fetchYandexManifest()?.let { payload ->
                payload to parseUpdateManifest(payload, UpdateDistributionSource.YANDEX_DISK)
            }
        }
        yandexResult.getOrNull()?.let { (payload, update) ->
            writeCache(payload, UpdateDistributionSource.YANDEX_DISK)
            return resultFor(update)
        }

        val githubResult = runCatching {
            fetchGithubManifest()?.let { payload ->
                payload to parseUpdateManifest(payload, UpdateDistributionSource.GITHUB)
            }
        }
        githubResult.getOrNull()?.let { (payload, update) ->
            writeCache(payload, UpdateDistributionSource.GITHUB)
            return resultFor(update)
        }

        if (githubResult.isSuccess) {
            // No manifest in either mirror means there is no published update yet.
            writeCache(null, null)
            return UpdateCheckResult.UpToDate(currentVersion)
        }

        cachedManifestOrNull()?.let { return resultFor(it) }
        val error = githubResult.exceptionOrNull() ?: yandexResult.exceptionOrNull()
        throw IllegalStateException(
            "Не удалось связаться с серверами обновлений. Проверьте интернет и повторите позже.",
            error
        )
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

        val candidates = buildList {
            if (asset.source == UpdateDistributionSource.YANDEX_DISK) {
                runCatching { resolveYandexPublicDownload("/${asset.name}") }
                    .onSuccess(::add)
                    .onFailure {
                        StartupLog.warning(
                            "Yandex Disk update download is unavailable; using GitHub fallback: " +
                                    it.message.orEmpty()
                        )
                    }
            }
            add(URI.create(asset.downloadUrl))
        }.distinctBy(URI::toString)

        var lastError: Throwable? = null
        for (downloadUri in candidates) {
            try {
                downloadAndVerify(asset, downloadUri, finalFile, onProgress)
                return finalFile
            } catch (error: Throwable) {
                lastError = error
                StartupLog.warning(
                    "Update download failed from ${downloadUri.host}; trying fallback: " +
                            error.message.orEmpty()
                )
            }
        }
        if (candidates.size == 1) throw checkNotNull(lastError)
        throw IllegalStateException(
            "Не удалось скачать обновление ни с Яндекс Диска, ни с GitHub.",
            lastError
        )
    }

    private fun downloadAndVerify(
        asset: UpdateInstallerAsset,
        downloadUri: URI,
        finalFile: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ) {
        val partialFile = File(updatesDirectory, finalFile.name + ".part")
        partialFile.delete()
        val request = HttpRequest.newBuilder(downloadUri)
            .timeout(Duration.ofMinutes(15))
            .header("Accept", "application/octet-stream")
            .header("User-Agent", "HolyPresenter/$currentVersion")
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() !in 200..299) {
            response.body().close()
            error("Не удалось скачать обновление: HTTP ${response.statusCode()}.")
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

    internal fun parseUpdateManifest(
        payload: String,
        source: UpdateDistributionSource = UpdateDistributionSource.GITHUB
    ): ApplicationUpdate {
        val root = json.parseToJsonElement(payload).jsonObject
        check(root["schemaVersion"]?.jsonPrimitive?.intOrNull == MANIFEST_SCHEMA_VERSION) {
            "Версия формата обновления не поддерживается."
        }
        val rawVersion = root.string("version") ?: error("В манифесте не указана версия.")
        val version = VersionNumber.parse(rawVersion).display
        val installerObject = root["installer"]?.jsonObject
            ?: error("В манифесте нет MSI-установщика HolyPresenter.")
        val digest = installerObject.string("sha256")?.lowercase()
            ?: error("У MSI-установщика нет SHA-256. Обновление заблокировано для безопасности.")
        check(SHA256_REGEX.matches(digest)) { "В манифесте указана неверная SHA-256 установщика." }

        return ApplicationUpdate(
            version = version,
            title = root.string("title")?.takeIf(String::isNotBlank) ?: "HolyPresenter $version",
            notes = root.string("notes").orEmpty().trim(),
            publishedAt = root.string("publishedAt"),
            releasePageUrl = root.string("releasePageUrl")
                ?: error("В манифесте нет ссылки на страницу выпуска."),
            installer = UpdateInstallerAsset(
                name = installerObject.string("name") ?: error("У установщика нет имени."),
                downloadUrl = installerObject.string("downloadUrl")
                    ?: error("У установщика нет ссылки для скачивания."),
                sizeBytes = installerObject["sizeBytes"]?.jsonPrimitive?.longOrNull
                    ?: error("У установщика не указан размер."),
                sha256 = digest,
                source = source
            )
        ).also { validateAsset(it.installer) }
    }

    private fun fetchYandexManifest(): String? {
        if (yandexPublicFolderUrl.isNullOrBlank()) return null
        val downloadUri = resolveYandexPublicDownload("/$UPDATE_MANIFEST_NAME")
        return downloadText(downloadUri, missingIsNull = true)
    }

    private fun fetchGithubManifest(): String? =
        downloadText(latestReleaseEndpoint, missingIsNull = true)

    private fun downloadText(uri: URI, missingIsNull: Boolean): String? {
        val request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(25))
            .header("Accept", "application/json")
            .header("User-Agent", "HolyPresenter/$currentVersion")
            .GET()
            .build()
        val response = httpClient.send(
            request,
            HttpResponse.BodyHandlers.ofString(Charsets.UTF_8)
        )
        if (missingIsNull && response.statusCode() == 404) return null
        check(response.statusCode() in 200..299) {
            "Сервер обновлений вернул HTTP ${response.statusCode()}."
        }
        check(response.body().toByteArray(Charsets.UTF_8).size <= MAX_MANIFEST_SIZE_BYTES) {
            "Манифест обновления слишком большой."
        }
        return response.body()
    }

    private fun resolveYandexPublicDownload(path: String): URI {
        val publicFolder = yandexPublicFolderUrl
            ?.takeIf(String::isNotBlank)
            ?: error("Публичная папка обновлений Яндекс Диска не настроена.")
        val separator = if (yandexPublicDownloadEndpoint.rawQuery == null) "?" else "&"
        val requestUri = URI.create(
            yandexPublicDownloadEndpoint.toString() + separator +
                    "public_key=${encoded(publicFolder)}&path=${encoded(path)}"
        )
        val request = HttpRequest.newBuilder(requestUri)
            .timeout(Duration.ofSeconds(25))
            .header("Accept", "application/json")
            .header("User-Agent", "HolyPresenter/$currentVersion")
            .GET()
            .build()
        val response = httpClient.send(
            request,
            HttpResponse.BodyHandlers.ofString(Charsets.UTF_8)
        )
        check(response.statusCode() in 200..299) {
            "Яндекс Диск не нашёл файл обновления ($path, HTTP ${response.statusCode()})."
        }
        val href = json.parseToJsonElement(response.body()).jsonObject.string("href")
            ?: error("Яндекс Диск не вернул ссылку для скачивания $path.")
        return URI.create(href).also(::validateYandexDownloadUri)
    }

    private fun resultFor(update: ApplicationUpdate): UpdateCheckResult =
        if (VersionNumber.parse(update.version) > VersionNumber.parse(currentVersion)) {
            UpdateCheckResult.Available(update)
        } else {
            UpdateCheckResult.UpToDate(currentVersion)
        }

    private fun cacheIsFresh(): Boolean {
        val checkedAt = runCatching {
            lastCheckFile.takeIf(File::isFile)
                ?.readText(Charsets.UTF_8)
                ?.trim()
                ?.toLongOrNull()
        }.getOrNull()
            ?: return false
        val age = nowEpochMillis() - checkedAt
        return age in 0 until CHECK_INTERVAL_MILLIS
    }

    private fun cachedResult(): UpdateCheckResult =
        cachedManifestOrNull()?.let(::resultFor)
            ?: UpdateCheckResult.UpToDate(currentVersion)

    private fun cachedManifestOrNull(): ApplicationUpdate? =
        cachedManifestFile.takeIf(File::isFile)?.let { file ->
            val source = runCatching {
                UpdateDistributionSource.valueOf(
                    cachedSourceFile.readText(Charsets.UTF_8).trim()
                )
            }.getOrDefault(UpdateDistributionSource.GITHUB)
            runCatching {
                parseUpdateManifest(file.readText(Charsets.UTF_8), source)
            }
                .onFailure { file.delete() }
                .getOrNull()
        }

    private fun writeCache(
        manifest: String?,
        source: UpdateDistributionSource?
    ) {
        updatesDirectory.mkdirs()
        if (manifest == null) {
            cachedManifestFile.delete()
            cachedSourceFile.delete()
        } else {
            writeTextAtomically(cachedManifestFile, manifest)
            writeTextAtomically(
                cachedSourceFile,
                checkNotNull(source).name
            )
        }
        writeTextAtomically(lastCheckFile, nowEpochMillis().toString())
    }

    private fun writeTextAtomically(destination: File, content: String) {
        val temporary = File(destination.parentFile, destination.name + ".tmp")
        temporary.writeText(content, Charsets.UTF_8)
        try {
            Files.move(
                temporary.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                temporary.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
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
            check(uri.scheme.equals("https", ignoreCase = true)) {
                "Ссылка на обновление должна использовать HTTPS."
            }
            check(uri.host.equals("github.com", ignoreCase = true)) {
                "Резервное обновление разрешено скачивать только с официального GitHub."
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
    private fun encoded(value: String): String = URLEncoder.encode(value, Charsets.UTF_8)

    private fun validateYandexDownloadUri(uri: URI) {
        if (!requireSecureUrls) return
        val host = uri.host.orEmpty().lowercase()
        check(uri.scheme.equals("https", ignoreCase = true)) {
            "Яндекс Диск вернул небезопасную ссылку для скачивания."
        }
        check(
            host == "yandex.ru" ||
                    host.endsWith(".yandex.ru") ||
                    host == "yandex.net" ||
                    host.endsWith(".yandex.net") ||
                    host == "yandex.com" ||
                    host.endsWith(".yandex.com")
        ) {
            "Яндекс Диск вернул ссылку с неизвестного сервера: $host"
        }
    }

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
            "https://github.com/maxx52/HolyPresenter/releases/latest/download/holypresenter-update.json"
        const val YANDEX_PUBLIC_FOLDER_URL = "https://disk.yandex.ru/d/74XmnWUi9hUCuQ"
        const val YANDEX_PUBLIC_DOWNLOAD_ENDPOINT =
            "https://cloud-api.yandex.net/v1/disk/public/resources/download"
        const val UPDATE_MANIFEST_NAME = "holypresenter-update.json"
        const val MANIFEST_SCHEMA_VERSION = 1
        const val MAX_MANIFEST_SIZE_BYTES = 256 * 1024
        const val CHECK_INTERVAL_MILLIS = 6L * 60L * 60L * 1000L
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
