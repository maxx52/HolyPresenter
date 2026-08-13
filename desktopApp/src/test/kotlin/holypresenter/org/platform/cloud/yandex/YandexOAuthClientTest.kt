package holypresenter.org.platform.cloud.yandex

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertEquals
import java.nio.file.Files

class YandexOAuthClientTest {
    @Test
    fun begin_usesAppFolderScopeAndPkce() {
        val pending = YandexOAuthClient().begin("client-id")

        assertContains(pending.authorizationUrl, "client_id=client-id")
        assertContains(pending.authorizationUrl, "cloud_api%3Adisk.app_folder")
        assertContains(pending.authorizationUrl, "code_challenge_method=S256")
        assertContains(pending.authorizationUrl, "verification_code")
    }

    @Test
    fun begin_createsNewVerifierForEveryAttempt() {
        val client = YandexOAuthClient()

        val first = client.begin("client-id")
        val second = client.begin("client-id")

        assertNotEquals(first.codeVerifier, second.codeVerifier)
        assertNotEquals(first.state, second.state)
    }

    @Test
    fun begin_rejectsBlankClientId() {
        assertFailsWith<IllegalArgumentException> {
            YandexOAuthClient().begin(" ")
        }
    }

    @Test
    fun cloudConfig_usesBundledClientIdByDefault() {
        val applicationHome = Files.createTempDirectory("holypresenter-yandex-config").toFile()
        try {
            assertEquals(
                "d0ccacd04e2c486781826ddcd8e23a24",
                YandexCloudConfig(applicationHome).clientId()
            )
        } finally {
            applicationHome.deleteRecursively()
        }
    }
}
