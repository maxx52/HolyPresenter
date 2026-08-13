package holypresenter.org.platform.ai

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiAssistantStorageTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun settings_roundTripAndUseSafeDefaults() {
        val storage = AiAssistantStorage(temporaryFolder.newFolder("settings-home"))

        assertEquals("gpt-5.6-luna", storage.loadSettings().textModel)
        assertEquals(10.0, storage.loadSettings().monthlyLimitUsd)

        val updated = storage.loadSettings().copy(monthlyLimitUsd = 75.0, videoSeconds = 8)
        storage.saveSettings(updated)

        assertEquals(updated, storage.loadSettings())
    }

    @Test
    fun budget_blocksRequestThatWouldExceedLimit() {
        val storage = AiAssistantStorage(temporaryFolder.newFolder("budget-home"))
        val month = YearMonth.of(2026, 8)

        storage.recordCost(9.75, month)

        assertTrue(storage.canSpend(0.25, 10.0, month))
        assertFalse(storage.canSpend(0.26, 10.0, month))
        assertEquals(9.75, storage.spentThisMonth(month))
    }
}
