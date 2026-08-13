package holypresenter.org.platform.ai

import holypresenter.org.platform.api.ai.AiGenerationRequest
import holypresenter.org.platform.api.ai.AiImageQuality
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FreeBackgroundProviderTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun generate_createsFreePngWithRequestedDimensions() = runBlocking {
        val provider = FreeBackgroundProvider(temporaryFolder.root)

        val result = provider.generate(
            AiGenerationRequest.Image(
                prompt = "Яркий молодёжный фон",
                model = "unused",
                quality = AiImageQuality.MEDIUM,
                size = "1536x1024"
            )
        )

        val file = File(result.filePath)
        val image = ImageIO.read(file)
        assertTrue(file.isFile)
        assertEquals(0.0, result.costUsd)
        assertEquals("image/png", result.mimeType)
        assertEquals(1_536, image.width)
        assertEquals(1_024, image.height)
    }
}
