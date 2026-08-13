package holypresenter.org.platform.ai

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.YearMonth

class AiAssistantStorage(
    applicationHome: File
) {
    private val settingsDirectory = File(applicationHome, "settings")
    private val settingsFile = File(settingsDirectory, "ai-assistant.json")
    private val usageFile = File(settingsDirectory, "ai-usage.json")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Synchronized
    fun loadSettings(): AiAssistantSettings =
        read(settingsFile) ?: AiAssistantSettings()

    @Synchronized
    fun saveSettings(settings: AiAssistantSettings) {
        write(settingsFile, settings)
    }

    @Synchronized
    fun spentThisMonth(now: YearMonth = YearMonth.now()): Double =
        loadLedger().monthCostsUsd[now.toString()] ?: 0.0

    @Synchronized
    fun canSpend(
        estimatedUsd: Double,
        monthlyLimitUsd: Double,
        now: YearMonth = YearMonth.now()
    ): Boolean = monthlyLimitUsd > 0.0 &&
            spentThisMonth(now) + estimatedUsd <= monthlyLimitUsd + COST_EPSILON

    @Synchronized
    fun recordCost(costUsd: Double, now: YearMonth = YearMonth.now()): Double {
        require(costUsd >= 0.0) { "Стоимость не может быть отрицательной" }
        val ledger = loadLedger()
        val key = now.toString()
        val total = (ledger.monthCostsUsd[key] ?: 0.0) + costUsd
        val retained = ledger.monthCostsUsd
            .filterKeys { month -> runCatching { YearMonth.parse(month) }.getOrNull()?.isAfter(now.minusMonths(13)) == true }
            .toMutableMap()
        retained[key] = total
        write(usageFile, AiUsageLedger(retained))
        return total
    }

    private fun loadLedger(): AiUsageLedger = read(usageFile) ?: AiUsageLedger()

    private inline fun <reified T> read(file: File): T? {
        if (!file.isFile) return null
        return runCatching { json.decodeFromString<T>(file.readText(Charsets.UTF_8)) }
            .getOrNull()
    }

    private inline fun <reified T> write(file: File, value: T) {
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, ".${file.name}.tmp")
        temporary.writeText(json.encodeToString(value), Charsets.UTF_8)
        runCatching {
            Files.move(
                temporary.toPath(),
                file.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        }.getOrElse {
            Files.move(
                temporary.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    private companion object {
        const val COST_EPSILON = 0.000_000_1
    }
}
