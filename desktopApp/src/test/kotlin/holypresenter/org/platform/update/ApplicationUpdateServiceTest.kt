package holypresenter.org.platform.update

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URI
import java.security.MessageDigest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApplicationUpdateServiceTest {
    @Test
    fun versionComparison_handlesPrefixesMissingPartsAndPrereleases() {
        assertTrue(VersionNumber.parse("v1.0.8") > VersionNumber.parse("1.0.7"))
        assertEquals(0, VersionNumber.parse("1.0").compareTo(VersionNumber.parse("1.0.0")))
        assertTrue(VersionNumber.parse("1.1.0") > VersionNumber.parse("1.0.99"))
        assertTrue(VersionNumber.parse("1.0.8") > VersionNumber.parse("1.0.8-beta"))
    }

    @Test
    fun checkAndDownload_acceptOnlyVerifiedMsi() {
        val installerBytes = "not-a-real-msi-but-valid-test-content".toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(installerBytes)
            .joinToString("") { "%02x".format(it) }
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val baseUrl = "http://127.0.0.1:${server.address.port}"
        val releaseJson = manifestJson(
            version = "1.0.8",
            assetUrl = "$baseUrl/HolyPresenter-1.0.8.msi",
            size = installerBytes.size.toLong(),
            digest = digest
        )
        server.createContext("/latest") { exchange ->
            exchange.sendResponseHeaders(200, releaseJson.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(releaseJson.toByteArray()) }
        }
        server.createContext("/HolyPresenter-1.0.8.msi") { exchange ->
            exchange.sendResponseHeaders(200, installerBytes.size.toLong())
            exchange.responseBody.use { it.write(installerBytes) }
        }
        server.start()

        val home = Files.createTempDirectory("holypresenter-update").toFile()
        try {
            val service = ApplicationUpdateService(
                applicationHome = home,
                currentVersion = "1.0.7",
                onExit = {},
                latestReleaseEndpoint = URI.create("$baseUrl/latest"),
                yandexPublicFolderUrl = null,
                requireSecureUrls = false
            )
            val result = assertIs<UpdateCheckResult.Available>(service.checkForUpdates())
            assertEquals("1.0.8", result.update.version)

            val progress = mutableListOf<Pair<Long, Long>>()
            val installer = service.download(result.update) { downloaded, total ->
                progress += downloaded to total
            }
            assertTrue(installer.isFile)
            assertTrue(installer.readBytes().contentEquals(installerBytes))
            assertEquals(installerBytes.size.toLong(), progress.last().first)
        } finally {
            server.stop(0)
            home.deleteRecursively()
        }
    }

    @Test
    fun parseUpdateManifest_rejectsInstallerWithoutDigest() {
        val service = ApplicationUpdateService(
            applicationHome = Files.createTempDirectory("holypresenter-update").toFile(),
            currentVersion = "1.0.7",
            onExit = {},
            requireSecureUrls = false
        )
        val error = assertFailsWith<IllegalStateException> {
            service.parseUpdateManifest(
                manifestJson("1.0.8", "http://localhost/update.msi", 10, null)
            )
        }
        assertContains(error.message.orEmpty(), "SHA-256")
    }

    @Test
    fun checkForUpdates_usesSixHourCacheAndForceRefreshBypassesIt() {
        val installerBytes = "cached-installer".toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(installerBytes)
            .joinToString("") { "%02x".format(it) }
        var requests = 0
        var now = 1_000_000L
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val baseUrl = "http://127.0.0.1:${server.address.port}"
        val payload = manifestJson(
            version = "1.0.8",
            assetUrl = "$baseUrl/HolyPresenter-1.0.8.msi",
            size = installerBytes.size.toLong(),
            digest = digest
        )
        server.createContext("/latest") { exchange ->
            requests++
            exchange.sendResponseHeaders(200, payload.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(payload.toByteArray()) }
        }
        server.start()
        val home = Files.createTempDirectory("holypresenter-update-cache").toFile()
        try {
            val service = ApplicationUpdateService(
                applicationHome = home,
                currentVersion = "1.0.7",
                onExit = {},
                latestReleaseEndpoint = URI.create("$baseUrl/latest"),
                yandexPublicFolderUrl = null,
                requireSecureUrls = false,
                nowEpochMillis = { now }
            )

            assertIs<UpdateCheckResult.Available>(service.checkForUpdates())
            assertIs<UpdateCheckResult.Available>(service.checkForUpdates())
            assertEquals(1, requests)

            assertIs<UpdateCheckResult.Available>(service.checkForUpdates(forceRefresh = true))
            assertEquals(2, requests)

            now += 6L * 60L * 60L * 1000L
            assertIs<UpdateCheckResult.Available>(service.checkForUpdates())
            assertEquals(3, requests)
        } finally {
            server.stop(0)
            home.deleteRecursively()
        }
    }

    @Test
    fun checkForUpdates_treatsMissingManifestAsCurrentVersionAndCachesIt() {
        var requests = 0
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/latest") { exchange ->
            requests++
            exchange.sendResponseHeaders(404, -1)
            exchange.close()
        }
        server.start()
        val home = Files.createTempDirectory("holypresenter-update-empty").toFile()
        try {
            val service = ApplicationUpdateService(
                applicationHome = home,
                currentVersion = "1.0.8",
                onExit = {},
                latestReleaseEndpoint = URI.create(
                    "http://127.0.0.1:${server.address.port}/latest"
                ),
                yandexPublicFolderUrl = null,
                requireSecureUrls = false,
                nowEpochMillis = { 1_000_000L }
            )

            assertIs<UpdateCheckResult.UpToDate>(service.checkForUpdates())
            assertIs<UpdateCheckResult.UpToDate>(service.checkForUpdates())
            assertEquals(1, requests)
        } finally {
            server.stop(0)
            home.deleteRecursively()
        }
    }

    @Test
    fun checkForUpdates_prefersYandexDiskAndDownloadsItsInstaller() {
        val installerBytes = "yandex-installer".toByteArray()
        val digest = sha256(installerBytes)
        var githubManifestRequests = 0
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val baseUrl = "http://127.0.0.1:${server.address.port}"
        val manifest = manifestJson(
            version = "1.0.9",
            assetUrl = "$baseUrl/HolyPresenter-1.0.9.msi",
            size = installerBytes.size.toLong(),
            digest = digest
        )
        server.createContext("/yandex-download") { exchange ->
            val href = if (exchange.requestURI.rawQuery.orEmpty().contains("holypresenter-update.json")) {
                "$baseUrl/yandex-manifest"
            } else {
                "$baseUrl/yandex-installer"
            }
            val response = "{\"href\":\"$href\"}".toByteArray()
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }
        server.createContext("/yandex-manifest") { exchange ->
            val response = manifest.toByteArray()
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }
        server.createContext("/yandex-installer") { exchange ->
            exchange.sendResponseHeaders(200, installerBytes.size.toLong())
            exchange.responseBody.use { it.write(installerBytes) }
        }
        server.createContext("/github-latest") { exchange ->
            githubManifestRequests++
            exchange.sendResponseHeaders(500, -1)
            exchange.close()
        }
        server.start()
        val home = Files.createTempDirectory("holypresenter-update-yandex").toFile()
        try {
            val service = ApplicationUpdateService(
                applicationHome = home,
                currentVersion = "1.0.8",
                onExit = {},
                latestReleaseEndpoint = URI.create("$baseUrl/github-latest"),
                yandexPublicFolderUrl = "https://disk.yandex.ru/d/test",
                yandexPublicDownloadEndpoint = URI.create("$baseUrl/yandex-download"),
                requireSecureUrls = false
            )

            val result = assertIs<UpdateCheckResult.Available>(service.checkForUpdates())
            assertEquals(UpdateDistributionSource.YANDEX_DISK, result.update.installer.source)
            assertEquals(0, githubManifestRequests)
            assertTrue(service.download(result.update).readBytes().contentEquals(installerBytes))
        } finally {
            server.stop(0)
            home.deleteRecursively()
        }
    }

    @Test
    fun checkForUpdates_fallsBackToGithubWhenYandexManifestIsInvalid() {
        val installerBytes = "github-installer".toByteArray()
        val manifest = manifestJson(
            version = "1.0.9",
            assetUrl = "http://localhost/HolyPresenter-1.0.9.msi",
            size = installerBytes.size.toLong(),
            digest = sha256(installerBytes)
        )
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val baseUrl = "http://127.0.0.1:${server.address.port}"
        server.createContext("/yandex-download") { exchange ->
            val response = "{\"href\":\"$baseUrl/invalid-manifest\"}".toByteArray()
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }
        server.createContext("/invalid-manifest") { exchange ->
            val response = "{}".toByteArray()
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }
        server.createContext("/github-latest") { exchange ->
            val response = manifest.toByteArray()
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }
        server.start()
        val home = Files.createTempDirectory("holypresenter-update-fallback").toFile()
        try {
            val service = ApplicationUpdateService(
                applicationHome = home,
                currentVersion = "1.0.8",
                onExit = {},
                latestReleaseEndpoint = URI.create("$baseUrl/github-latest"),
                yandexPublicFolderUrl = "https://disk.yandex.ru/d/test",
                yandexPublicDownloadEndpoint = URI.create("$baseUrl/yandex-download"),
                requireSecureUrls = false
            )

            val result = assertIs<UpdateCheckResult.Available>(service.checkForUpdates())
            assertEquals(UpdateDistributionSource.GITHUB, result.update.installer.source)
        } finally {
            server.stop(0)
            home.deleteRecursively()
        }
    }

    @Test
    fun download_fallsBackToGithubWhenYandexInstallerIsMissing() {
        val installerBytes = "fallback-installer".toByteArray()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val baseUrl = "http://127.0.0.1:${server.address.port}"
        server.createContext("/yandex-download") { exchange ->
            exchange.sendResponseHeaders(404, -1)
            exchange.close()
        }
        server.createContext("/HolyPresenter-1.0.9.msi") { exchange ->
            exchange.sendResponseHeaders(200, installerBytes.size.toLong())
            exchange.responseBody.use { it.write(installerBytes) }
        }
        server.start()
        val home = Files.createTempDirectory("holypresenter-update-download-fallback").toFile()
        try {
            val service = ApplicationUpdateService(
                applicationHome = home,
                currentVersion = "1.0.8",
                onExit = {},
                yandexPublicFolderUrl = "https://disk.yandex.ru/d/test",
                yandexPublicDownloadEndpoint = URI.create("$baseUrl/yandex-download"),
                requireSecureUrls = false
            )
            val update = ApplicationUpdate(
                version = "1.0.9",
                title = "HolyPresenter 1.0.9",
                notes = "",
                publishedAt = null,
                releasePageUrl = "https://github.com/maxx52/HolyPresenter/releases/tag/v1.0.9",
                installer = UpdateInstallerAsset(
                    name = "HolyPresenter-1.0.9.msi",
                    downloadUrl = "$baseUrl/HolyPresenter-1.0.9.msi",
                    sizeBytes = installerBytes.size.toLong(),
                    sha256 = sha256(installerBytes),
                    source = UpdateDistributionSource.YANDEX_DISK
                )
            )

            assertTrue(service.download(update).readBytes().contentEquals(installerBytes))
        } finally {
            server.stop(0)
            home.deleteRecursively()
        }
    }

    @Test
    fun download_rejectsChangedFileAndRemovesPartialDownload() {
        val expectedBytes = "expected-content".toByteArray()
        val changedBytes = "tampered-content".toByteArray()
        assertEquals(expectedBytes.size, changedBytes.size)
        val expectedDigest = MessageDigest.getInstance("SHA-256")
            .digest(expectedBytes)
            .joinToString("") { "%02x".format(it) }
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val url = "http://127.0.0.1:${server.address.port}/HolyPresenter-1.0.8.msi"
        server.createContext("/HolyPresenter-1.0.8.msi") { exchange ->
            exchange.sendResponseHeaders(200, changedBytes.size.toLong())
            exchange.responseBody.use { it.write(changedBytes) }
        }
        server.start()
        val home = Files.createTempDirectory("holypresenter-update-corrupt").toFile()
        try {
            val service = ApplicationUpdateService(
                applicationHome = home,
                currentVersion = "1.0.7",
                onExit = {},
                requireSecureUrls = false
            )
            val update = ApplicationUpdate(
                version = "1.0.8",
                title = "HolyPresenter 1.0.8",
                notes = "",
                publishedAt = null,
                releasePageUrl = "https://github.com/maxx52/HolyPresenter/releases/tag/v1.0.8",
                installer = UpdateInstallerAsset(
                    name = "HolyPresenter-1.0.8.msi",
                    downloadUrl = url,
                    sizeBytes = changedBytes.size.toLong(),
                    sha256 = expectedDigest
                )
            )

            val error = assertFailsWith<IllegalStateException> { service.download(update) }
            assertContains(error.message.orEmpty(), "Контрольная сумма")
            val updates = home.resolve("updates")
            assertFalse(updates.resolve("HolyPresenter-1.0.8.msi").exists())
            assertFalse(updates.resolve("HolyPresenter-1.0.8.msi.part").exists())
        } finally {
            server.stop(0)
            home.deleteRecursively()
        }
    }

    @Test
    fun installScript_waitsForAppAndEscapesPaths() {
        val service = ApplicationUpdateService(
            applicationHome = Files.createTempDirectory("holypresenter-update").toFile(),
            currentVersion = "1.0.7",
            onExit = {},
            requireSecureUrls = false
        )
        val script = service.buildInstallScript(
            processId = 42,
            installerPath = "C:\\Users\\O'Brien\\update.msi",
            applicationPath = "C:\\Program Files\\HolyPresenter\\HolyPresenter.exe"
        )
        assertContains(script, "Wait-Process -Id 42")
        assertContains(script, "O''Brien")
        assertContains(script, "'/passive'")
        assertContains(script, "-Verb RunAs")
        assertContains(script, "HolyPresenter.exe")
        assertContains(script, "last-update-error.txt")
        assertContains(script, "finally")
    }

    private fun manifestJson(
        version: String,
        assetUrl: String,
        size: Long,
        digest: String?
    ): String = """
        {
          "schemaVersion": 1,
          "version": "$version",
          "title": "HolyPresenter $version",
          "notes": "Новая версия",
          "releasePageUrl": "https://github.com/maxx52/HolyPresenter/releases/tag/v$version",
          "publishedAt": "2026-08-13T12:00:00Z",
          "installer": {
              "name": "HolyPresenter-$version.msi",
              "downloadUrl": "$assetUrl",
              "sizeBytes": $size${digest?.let { ",\n              \"sha256\": \"$it\"" }.orEmpty()}
          }
        }
    """.trimIndent()

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }
}
