package holypresenter.org.platform.cloud.yandex

import kotlinx.serialization.json.Json
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class YandexDiskClient(
    private val accessToken: String,
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build()
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun listBackups(): List<YandexRemoteBackup> {
        val response = sendJson(
            HttpRequest.newBuilder(apiUri("/resources", mapOf(
                "path" to BACKUPS_PATH,
                "limit" to "100",
                "sort" to "-modified"
            ))).GET().build(),
            accepted = setOf(200, 404)
        )
        if (response.statusCode() == 404) return emptyList()
        return json.decodeFromString<YandexResourceListResponse>(response.body())
            .embedded.orEmptyItems()
            .filter { it.type == "file" && it.name.endsWith(".holybackup", ignoreCase = true) }
            .map { resource ->
                YandexRemoteBackup(
                    name = resource.name,
                    path = resource.path,
                    size = resource.size,
                    modified = resource.modified
                )
            }
    }

    fun upload(backup: File) {
        require(backup.isFile) { "Файл резервной копии не найден" }
        ensureBackupsDirectory()
        val link = requestLink(
            "/resources/upload",
            mapOf(
                "path" to "$BACKUPS_PATH/${backup.name}",
                "overwrite" to "true"
            )
        )
        val upload = HttpRequest.newBuilder(URI.create(link.href))
            .timeout(Duration.ofMinutes(30))
            .PUT(HttpRequest.BodyPublishers.ofFile(backup.toPath()))
            .build()
        val response = httpClient.send(upload, HttpResponse.BodyHandlers.discarding())
        check(response.statusCode() in 200..299) {
            "Яндекс Диск не принял резервную копию (${response.statusCode()})"
        }
    }

    fun download(remote: YandexRemoteBackup, destination: File) {
        val link = requestLink("/resources/download", mapOf("path" to remote.path))
        destination.parentFile?.mkdirs()
        val temporary = File(destination.parentFile, ".${destination.name}.tmp")
        temporary.delete()
        val request = HttpRequest.newBuilder(URI.create(link.href))
            .timeout(Duration.ofMinutes(30))
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(temporary.toPath()))
        check(response.statusCode() in 200..299) {
            temporary.delete()
            "Не удалось скачать резервную копию (${response.statusCode()})"
        }
        temporary.copyTo(destination, overwrite = true)
        temporary.delete()
    }

    fun delete(remote: YandexRemoteBackup) {
        val request = authorized(
            HttpRequest.newBuilder(apiUri("/resources", mapOf(
                "path" to remote.path,
                "permanently" to "true"
            ))).DELETE()
        ).build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() in setOf(202, 204, 404)) {
            "Не удалось удалить резервную копию (${response.statusCode()}): ${response.body().take(300)}"
        }
    }

    private fun ensureBackupsDirectory() {
        val request = authorized(
            HttpRequest.newBuilder(apiUri("/resources", mapOf("path" to BACKUPS_PATH))).PUT(
                HttpRequest.BodyPublishers.noBody()
            )
        ).build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() in setOf(201, 409)) {
            "Не удалось подготовить папку на Яндекс Диске (${response.statusCode()}): ${response.body().take(300)}"
        }
    }

    private fun requestLink(path: String, parameters: Map<String, String>): YandexLinkResponse {
        val response = sendJson(
            HttpRequest.newBuilder(apiUri(path, parameters)).GET().build(),
            accepted = setOf(200)
        )
        return json.decodeFromString(response.body())
    }

    private fun sendJson(
        request: HttpRequest,
        accepted: Set<Int>
    ): HttpResponse<String> {
        val response = httpClient.send(
            authorized(HttpRequest.newBuilder(request.uri()).method(
                request.method(),
                request.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody())
            )).build(),
            HttpResponse.BodyHandlers.ofString()
        )
        check(response.statusCode() in accepted) {
            "Ошибка Яндекс Диска (${response.statusCode()}): ${response.body().take(500)}"
        }
        return response
    }

    private fun authorized(builder: HttpRequest.Builder): HttpRequest.Builder =
        builder.timeout(Duration.ofSeconds(30))
            .header("Authorization", "OAuth $accessToken")
            .header("Accept", "application/json")

    private fun apiUri(path: String, parameters: Map<String, String>): URI {
        val query = parameters.entries.joinToString("&") { (name, value) ->
            "${encode(name)}=${encode(value)}"
        }
        return URI.create("$API_ROOT$path?$query")
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8)

    private fun YandexEmbeddedResources?.orEmptyItems(): List<YandexResource> =
        this?.items.orEmpty()

    private companion object {
        const val API_ROOT = "https://cloud-api.yandex.net/v1/disk"
        const val BACKUPS_PATH = "app:/backups"
    }
}
