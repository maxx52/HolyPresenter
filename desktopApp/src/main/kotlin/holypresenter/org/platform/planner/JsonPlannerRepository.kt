package holypresenter.org.platform.planner

import holypresenter.org.platform.api.planner.PlannerItem
import holypresenter.org.platform.api.planner.PlannerReference
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal class JsonPlannerRepository(
    private val plannerDirectory: File,
    private val legacyPlannerFile: File? = null
) {
    private val currentFile =
        File(
            plannerDirectory,
            "current.json"
        )

    private val plansDirectory =
        File(
            plannerDirectory,
            "plans"
        )

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    init {
        plannerDirectory.mkdirs()
        plansDirectory.mkdirs()
    }

    fun loadCurrent(): PlannerSnapshot {
        if (currentFile.isFile) {
            return readSnapshot(currentFile)
                ?: PlannerSnapshot()
        }

        return migrateLegacyPlanner()
            ?: PlannerSnapshot()
    }

    fun saveCurrent(
        snapshot: PlannerSnapshot
    ) {
        writeSnapshot(
            file = currentFile,
            snapshot = snapshot
        )
    }

    fun listPlans():
            List<PlannerSnapshot> =
        plansDirectory
            .listFiles { file ->
                file.isFile &&
                        file.extension.equals(
                            other = "json",
                            ignoreCase = true
                        )
            }
            .orEmpty()
            .mapNotNull { file ->
                readSnapshot(file)
            }
            .filter { snapshot ->
                snapshot.id != null &&
                        !snapshot.name.isNullOrBlank()
            }
            .sortedBy { snapshot ->
                snapshot.name
                    ?.lowercase()
                    .orEmpty()
            }

    fun loadPlan(
        planId: String
    ): PlannerSnapshot? {
        val file =
            planFile(planId)
                ?: return null

        return readSnapshot(file)
    }

    fun savePlan(
        snapshot: PlannerSnapshot
    ) {
        val planId =
            snapshot.id
                ?: return

        val file =
            planFile(planId)
                ?: return

        writeSnapshot(
            file = file,
            snapshot = snapshot
        )
    }

    private fun migrateLegacyPlanner():
            PlannerSnapshot? {
        val sourceFile =
            legacyPlannerFile
                ?.takeIf { it.isFile }
                ?: return null

        /*
         * Старый planner.json не содержал
         * id и name, но у новых полей есть
         * значения по умолчанию.
         */
        val snapshot =
            readSnapshot(sourceFile)
                ?: return null

        saveCurrent(snapshot)

        val backupFile =
            File(
                sourceFile.parentFile,
                sourceFile.nameWithoutExtension +
                        ".legacy.json"
            )

        runCatching {
            Files.move(
                sourceFile.toPath(),
                backupFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }.onFailure { error ->
            println(
                "Failed to move legacy planner file: " +
                        error.message
            )
        }

        return snapshot
    }

    private fun readSnapshot(
        file: File
    ): PlannerSnapshot? {
        if (!file.isFile) {
            return null
        }

        return runCatching {
            json.decodeFromString<PlannerSnapshot>(
                file.readText()
            )
        }.onFailure { error ->
            println(
                "Failed to load planner file " +
                        "${file.name}: " +
                        error.message
            )
        }.getOrNull()
    }

    private fun writeSnapshot(
        file: File,
        snapshot: PlannerSnapshot
    ) {
        file.parentFile?.mkdirs()

        val temporaryFile =
            File(
                file.absolutePath + ".tmp"
            )

        temporaryFile.writeText(
            json.encodeToString(snapshot)
        )

        runCatching {
            Files.move(
                temporaryFile.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        }.recoverCatching {
            Files.move(
                temporaryFile.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }.getOrThrow()
    }

    private fun planFile(
        planId: String
    ): File? {
        val validId =
            planId.isNotBlank() &&
                    planId.all { character ->
                        character.isLetterOrDigit() ||
                                character == '-' ||
                                character == '_'
                    }

        if (!validId) {
            return null
        }

        return File(
            plansDirectory,
            "$planId.json"
        )
    }
}

@Serializable
internal data class PlannerSnapshot(
    val id: String? = null,
    val name: String? = null,
    val items: List<PlannerItemSnapshot> =
        emptyList(),
    val activeItemIndex: Int? = null,
    val updatedAtEpochMillis: Long = 0L
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
                        moduleId =
                            item.reference.moduleId,
                        itemId =
                            item.reference.itemId,
                        title = item.title
                    )
            }
    }
}