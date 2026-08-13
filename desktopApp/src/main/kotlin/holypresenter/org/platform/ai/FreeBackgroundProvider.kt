package holypresenter.org.platform.ai

import holypresenter.org.platform.api.ai.AiCostEstimate
import holypresenter.org.platform.api.ai.AiGenerationKind
import holypresenter.org.platform.api.ai.AiGenerationRequest
import holypresenter.org.platform.api.ai.AiGenerationResult
import holypresenter.org.platform.api.ai.AiProvider
import holypresenter.org.platform.api.ai.AiTokenUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Color
import java.awt.GradientPaint
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Random
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.math.max

/** Creates attractive lightweight backgrounds without a model, network, or GPU. */
class FreeBackgroundProvider(
    applicationHome: File
) : AiProvider {
    override val id: String = "free-backgrounds"
    override val displayName: String = "Бесплатные фоны"
    override val supportedKinds: Set<AiGenerationKind> = setOf(AiGenerationKind.IMAGE)

    private val generatedDirectory = File(applicationHome, "ai/generated")

    override fun estimate(request: AiGenerationRequest): AiCostEstimate {
        require(request is AiGenerationRequest.Image) {
            "Бесплатные фоны поддерживают только изображения"
        }
        return AiCostEstimate(0.0, "бесплатно · мгновенно · без нейросети")
    }

    override suspend fun generate(
        request: AiGenerationRequest,
        onProgress: (percent: Int, message: String) -> Unit
    ): AiGenerationResult.Media = withContext(Dispatchers.Default) {
        require(request is AiGenerationRequest.Image) {
            "Бесплатные фоны поддерживают только изображения"
        }
        require(request.prompt.isNotBlank()) { "Опишите желаемый фон" }
        onProgress(15, "Подбираем цвета…")
        val (width, height) = imageSize(request.size)
        val image = render(request.prompt, width, height)
        onProgress(80, "Сохраняем фон…")
        generatedDirectory.mkdirs()
        val destination = File(
            generatedDirectory,
            "free-background-${FILE_TIME_FORMAT.format(LocalDateTime.now())}-" +
                "${UUID.randomUUID().toString().take(8)}.png"
        )
        check(ImageIO.write(image, "png", destination)) { "Не удалось сохранить PNG-фон" }
        image.flush()
        onProgress(100, "Бесплатный фон готов")
        AiGenerationResult.Media(
            providerId = id,
            filePath = destination.absolutePath,
            mimeType = "image/png",
            kind = AiGenerationKind.IMAGE,
            costUsd = 0.0,
            usage = AiTokenUsage()
        )
    }

    internal fun render(prompt: String, width: Int, height: Int): BufferedImage {
        require(width in 256..2_048 && height in 256..2_048)
        val random = Random(prompt.lowercase().hashCode().toLong())
        val palette = palette(prompt)
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.paint = GradientPaint(
                0f,
                0f,
                palette.first,
                width.toFloat(),
                height.toFloat(),
                palette.second
            )
            graphics.fillRect(0, 0, width, height)

            repeat(12) { index ->
                val diameter = max(width, height) * (0.18 + random.nextDouble() * 0.42)
                val x = random.nextDouble() * width - diameter / 2
                val y = random.nextDouble() * height - diameter / 2
                val base = if (index % 2 == 0) palette.accent else Color.WHITE
                graphics.color = Color(base.red, base.green, base.blue, 18 + random.nextInt(42))
                graphics.fill(Ellipse2D.Double(x, y, diameter, diameter))
            }

            graphics.paint = GradientPaint(
                0f,
                0f,
                Color(0, 0, 0, 25),
                0f,
                height.toFloat(),
                Color(0, 0, 0, 95)
            )
            graphics.fillRect(0, 0, width, height)
        } finally {
            graphics.dispose()
        }
        return image
    }

    private fun imageSize(value: String): Pair<Int, Int> {
        val parts = value.lowercase().split('x')
        val width = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(512, 2_048) ?: 1_536
        val height = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(512, 2_048) ?: 1_024
        return width to height
    }

    private fun palette(prompt: String): Palette {
        val value = prompt.lowercase()
        return when {
            listOf("молод", "ярк", "празд", "дет").any(value::contains) ->
                Palette(Color(255, 80, 125), Color(62, 32, 160), Color(255, 210, 65))
            listOf("природ", "лес", "весн", "жизн").any(value::contains) ->
                Palette(Color(18, 130, 104), Color(7, 38, 82), Color(138, 235, 170))
            listOf("рассвет", "солн", "тепл", "свет").any(value::contains) ->
                Palette(Color(251, 157, 80), Color(87, 43, 125), Color(255, 226, 132))
            listOf("поклон", "молит", "спокой", "стих").any(value::contains) ->
                Palette(Color(22, 58, 112), Color(17, 17, 47), Color(95, 184, 255))
            else -> Palette(Color(44, 89, 180), Color(101, 39, 128), Color(255, 112, 166))
        }
    }

    private data class Palette(
        val first: Color,
        val second: Color,
        val accent: Color
    )

    private companion object {
        val FILE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    }
}
