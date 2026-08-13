package holypresenter.org.platform.cloud.yandex

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DpapiYandexTokenStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun saveAndLoad_roundTripsProtectedToken() {
        val home = temporaryFolder.newFolder("HolyPresenter")
        val store = DpapiYandexTokenStore(home, ReversingProtector)
        val token = YandexOAuthToken(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            expiresInSeconds = 3_600,
            obtainedAtEpochMillis = 123_456
        )

        store.save(token)

        val storedFile = home.resolve("cloud/yandex-token.dpapi")
        assertTrue(storedFile.isFile)
        assertFalse(storedFile.readText().contains("access-token"))
        assertEquals(token, store.load())
    }

    @Test
    fun clear_removesStoredCredentials() {
        val home = temporaryFolder.newFolder("ClearHome")
        val store = DpapiYandexTokenStore(home, ReversingProtector)
        store.save(YandexOAuthToken(accessToken = "token"))

        store.clear()

        assertNull(store.load())
        assertFalse(home.resolve("cloud/yandex-token.dpapi").exists())
    }

    private object ReversingProtector : TokenProtector {
        override fun protect(value: ByteArray): ByteArray = value.reversedArray()
        override fun unprotect(value: ByteArray): ByteArray = value.reversedArray()
    }
}
