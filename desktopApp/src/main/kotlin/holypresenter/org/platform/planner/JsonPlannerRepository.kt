package holypresenter.org.platform.planner

import holypresenter.org.platform.api.planner.PlannerItem
import holypresenter.org.platform.api.planner.PlannerReference
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal class JsonPlannerRepository(
    private val plannerFile: File
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    init {
        plannerFile.parentFile?.mkdirs()
    }

    fun load(): PlannerSnapshot {
        if (!plannerFile.isFile) {
            return PlannerSnapshot()
        }

        return runCatching {
            json.decodeFromString<PlannerSnapshot>(plannerFile.readText())
        }.onFailure { error ->
            println("Failed to load planner: + ${error.message}")
        }.getOrDefault(
            PlannerSnapshot()
        )
    }

    fun save(snapshot: PlannerSnapshot) {
        plannerFile.parentFile?.mkdirs()

        val temporaryFile = File(
            plannerFile.absolutePath + ".tmp"
        )

        temporaryFile.writeText(
            json.encodeToString(snapshot)
        )
        /*
         * Сначала пробуем атомарную замену.
         * На файловых системах без ATOMIC_MOVE
         * выполняем обычную замену.
         */
        runCatching {
            Files.move(
                temporaryFile.toPath(),
                plannerFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        }.recoverCatching {
            Files.move(
                temporaryFile.toPath(),
                plannerFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }.getOrThrow()
    }
}

@Serializable
internal data class PlannerSnapshot(
    val items: List<PlannerItemSnapshot> = emptyList(),
    val activeItemIndex: Int? = null
)

@Serializable
internal data class PlannerItemSnapshot(
    val moduleId: String,
    val itemId: String,
    val title: String
) {
    fun toPlannerItem(): PlannerItem =
        PlannerItem.Generic(
            reference = PlannerReference(
                moduleId = moduleId,
                itemId = itemId
            ),
            title = title
        )

    companion object {
        fun from(
            item: PlannerItem
        ): PlannerItemSnapshot =
            when (item) {
                is PlannerItem.Generic ->
                    PlannerItemSnapshot(
                        moduleId = item.reference.moduleId,
                        itemId = item.reference.itemId,
                        title = item.title
                    )
            }
    }
}