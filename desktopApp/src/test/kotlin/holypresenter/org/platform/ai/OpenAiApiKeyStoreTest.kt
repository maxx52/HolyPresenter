package holypresenter.org.platform.ai

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OpenAiApiKeyStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun save_encryptsKeyAndRoundTrips() {
        val home = temporaryFolder.newFolder("key-home")
        val store = OpenAiApiKeyStore(home, ReversingAiProtector)
        val key = fakeApiKey()

        store.save(key)

        val storedFile = home.resolve("cloud/openai-api-key.dpapi")
        assertTrue(storedFile.isFile)
        assertFalse(storedFile.readText().contains(key))
        assertEquals(key, store.load())
    }

    @Test
    fun clear_removesSavedKey() {
        val home = temporaryFolder.newFolder("clear-key-home")
        val store = OpenAiApiKeyStore(home, ReversingAiProtector)
        store.save(fakeApiKey())

        store.clear()

        assertNull(store.load())
    }

    private object ReversingAiProtector : AiSecretProtector {
        override fun protect(value: ByteArray): ByteArray = value.reversedArray()
        override fun unprotect(value: ByteArray): ByteArray = value.reversedArray()
    }

    private fun fakeApiKey(): String =
        "sk-" + "not-a-real-key-for-tests"
}
