package holypresenter.org.platform.layout.repository

import holypresenter.org.platform.layout.AppLayout
import kotlinx.serialization.json.Json
import java.io.File

class JsonLayoutRepository(
    private val layoutDirectory: File
) : LayoutRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    init {
        if (!layoutDirectory.exists()) {
            layoutDirectory.mkdirs()
        }
    }

    override fun load(name: String): AppLayout? {
        val file = File(layoutDirectory, "$name.json")

        if (!file.exists()) {
            return null
        }

        return json.decodeFromString<AppLayout>(
            file.readText()
        )
    }

    override fun save(layout: AppLayout) {
        val file = File(layoutDirectory, "${layout.name}.json")

        file.writeText(
            json.encodeToString(layout)
        )
    }

    override fun delete(name: String) {
        File(layoutDirectory, "$name.json")
            .takeIf { it.exists() }
            ?.delete()
    }

    override fun list(): List<String> {
        return layoutDirectory
            .listFiles { file ->
                file.extension == "json"
            }
            ?.map { it.nameWithoutExtension }
            ?.sorted()
            ?: emptyList()
    }
}