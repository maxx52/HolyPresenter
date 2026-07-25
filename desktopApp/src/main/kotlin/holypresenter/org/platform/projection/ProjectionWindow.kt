package holypresenter.org.platform.projection

import holypresenter.org.platform.api.presentation.element.TextElement
import holypresenter.org.platform.api.presentation.theme.PresentationBackgroundType
import holypresenter.org.platform.api.presentation.theme.PresentationTextStyle
import holypresenter.org.platform.api.presentation.theme.PresentationTheme
import holypresenter.org.platform.api.projection.ProjectionContent
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import kotlin.math.max

internal class ProjectionWindow(
    private val onClose: () -> Unit
) {
    private val projectionPanel = ProjectionPanel()

    private val frame = JFrame().apply {
        title = "HolyPresenter Projection"

        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE

        background = Color.BLACK
        contentPane = projectionPanel

        rootPane.registerKeyboardAction(
            { onClose() },
            KeyStroke.getKeyStroke("ESCAPE"),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        )

        addWindowListener(
            object : WindowAdapter() {
                override fun windowClosing(
                    event: WindowEvent
                ) {
                    onClose()
                }
            }
        )
    }

    fun show(
        content: ProjectionContent
    ) {
        runOnSwingThread {
            projectionPanel.updateContent(content)
            showOnProjector()
        }
    }

    fun close() {
        runOnSwingThread {
            frame.isVisible = false
        }
    }

    private fun showOnProjector() {
        val devices = GraphicsEnvironment
            .getLocalGraphicsEnvironment()
            .screenDevices

        frame.dispose()

        if (devices.size > 1) {
            val projectorBounds = devices[1]
                .defaultConfiguration
                .bounds

            frame.isUndecorated = true
            frame.isAlwaysOnTop = true
            frame.bounds = projectorBounds
        } else {
            frame.isUndecorated = false
            frame.isAlwaysOnTop = false
            frame.setSize(960, 540)
            frame.setLocationRelativeTo(null)
        }
        frame.isVisible = true
        frame.validate()
        frame.toFront()
        frame.requestFocus()
    }

    private fun runOnSwingThread(
        action: () -> Unit
    ) {
        if (SwingUtilities.isEventDispatchThread()) {
            action()
        } else {
            SwingUtilities.invokeLater(action)
        }
    }
}

private class ProjectionPanel : JPanel() {
    private var content: ProjectionContent = ProjectionContent.Empty
    private var backgroundImage: BufferedImage? = null

    init {
        background = Color.BLACK
        isOpaque = true
    }

    fun updateContent(
        content: ProjectionContent
    ) {
        this.content = content
        backgroundImage =
            loadBackgroundImage(content)
        repaint()
    }

    override fun paintComponent(
        graphics: Graphics
    ) {
        super.paintComponent(graphics)

        val graphics2D = graphics.create() as Graphics2D

        try {
            configureRendering(graphics2D)
            clearCanvas(graphics2D)

            when (val current = content) {
                ProjectionContent.Empty -> Unit
                ProjectionContent.BlackScreen -> Unit
                ProjectionContent.Logo -> Unit
                is ProjectionContent.Slide ->
                    drawSlide(
                        graphics = graphics2D,
                        content = current
                    )
            }
        } finally {
            graphics2D.dispose()
        }
    }

    private fun configureRendering(
        graphics: Graphics2D
    ) {
        graphics.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        )

        graphics.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        )

        graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC
        )
    }

    private fun clearCanvas(
        graphics: Graphics2D
    ) {
        graphics.color = Color.BLACK
        graphics.fillRect(0, 0, width, height)
    }

    private fun drawSlide(
        graphics: Graphics2D,
        content: ProjectionContent.Slide
    ) {
        val slide = content.slide ?: return
        val theme = content.presentation.theme

        drawBackground(graphics, theme)
        drawOverlay(graphics, theme)

        val text = slide.elements
            .asSequence()
            .filter { it.visible }
            .sortedBy { it.zIndex }
            .filterIsInstance<TextElement>()
            .joinToString("\n") { element -> element.text }

        drawText(
            graphics = graphics,
            text = text,
            style = theme.textStyle
        )
    }

    private fun drawBackground(
        graphics: Graphics2D,
        theme: PresentationTheme
    ) {
        when (theme.background.type) {
            PresentationBackgroundType.COLOR -> {
                graphics.color = theme
                    .background
                    .color
                    ?.toAwtColor()
                    ?: Color.BLACK
                graphics.fillRect(0, 0, width, height)
            }

            PresentationBackgroundType.IMAGE -> {
                drawBackgroundImage(graphics)
            }

            PresentationBackgroundType.VIDEO -> {
                /*
                 * Видео будет подключено через уже существующий
                 * VideoPlaybackService на следующем этапе.
                 */
            }
        }
    }

    private fun drawBackgroundImage(
        graphics: Graphics2D
    ) {
        val image = backgroundImage ?: return

        val scale = max(
            width.toDouble() / image.width,
            height.toDouble() / image.height
        )

        val targetWidth = (image.width * scale).toInt()
        val targetHeight = (image.height * scale).toInt()
        val x = (width - targetWidth) / 2
        val y = (height - targetHeight) / 2

        graphics.drawImage(
            image,
            x,
            y,
            targetWidth,
            targetHeight,
            null
        )
    }

    private fun drawOverlay(
        graphics: Graphics2D,
        theme: PresentationTheme
    ) {
        val overlay = theme.overlay

        if (!overlay.enabled) return

        val opacity = overlay.opacity.coerceIn(0f, 1f)

        if (opacity <= 0f) return

        val alpha = (opacity * 255).toInt()

        graphics.color = Color(0, 0, 0, alpha)

        graphics.fillRect(
            0,
            0,
            width,
            height
        )
    }

    private fun drawText(
        graphics: Graphics2D,
        text: String,
        style: PresentationTextStyle
    ) {
        if (text.isBlank()) return

        graphics.font = Font(
            style.fontFamily
                ?.takeIf(String::isNotBlank)
                ?: Font.SANS_SERIF,
            style.toFontStyle(),
            style.fontSize.coerceAtLeast(12)
        )

        val lines = text.lines()
        val metrics = graphics.fontMetrics
        val lineHeight = metrics.height
        val totalHeight = lines.size * lineHeight
        var y = (height - totalHeight) / 2 + metrics.ascent

        lines.forEach { line ->
            val x = (width - metrics.stringWidth(line)) / 2

            if (style.shadowEnabled) {
                drawTextShadow(
                    graphics = graphics,
                    text = line,
                    x = x,
                    y = y
                )
            }

            if (style.outlineEnabled) {
                drawTextOutline(
                    graphics = graphics,
                    text = line,
                    x = x,
                    y = y
                )
            }

            graphics.color = style.textColor.toAwtColor()

            graphics.drawString(
                line,
                x,
                y
            )
            y += lineHeight
        }
    }

    private fun drawTextShadow(
        graphics: Graphics2D,
        text: String,
        x: Int,
        y: Int
    ) {
        graphics.color = Color(0, 0, 0, 170)

        graphics.drawString(
            text,
            x + 4,
            y + 4
        )
    }

    private fun drawTextOutline(
        graphics: Graphics2D,
        text: String,
        x: Int,
        y: Int
    ) {
        graphics.color = Color.BLACK

        for (offsetX in -2..2) {
            for (offsetY in -2..2) {
                if (
                    offsetX == 0 &&
                    offsetY == 0
                ) {
                    continue
                }

                graphics.drawString(
                    text,
                    x + offsetX,
                    y + offsetY
                )
            }
        }
    }

    private fun loadBackgroundImage(
        content: ProjectionContent
    ): BufferedImage? {
        val slideContent = content as? ProjectionContent.Slide ?: return null

        val background = slideContent
                .presentation
                .theme
                .background

        if (
            background.type !=
            PresentationBackgroundType.IMAGE
        ) {
            return null
        }

        val path = background.path
            ?.takeIf(String::isNotBlank)
            ?: return null

        return runCatching {
            val file = File(path)

            if (!file.isFile) {
                return@runCatching null
            }

            ImageIO.read(file)
        }.getOrNull()
    }
}

private fun PresentationTextStyle.toFontStyle(): Int =
    when {
        bold && italic -> Font.BOLD or Font.ITALIC
        bold -> Font.BOLD
        italic -> Font.ITALIC
        else -> Font.PLAIN
    }

private fun Long.toAwtColor(): Color = Color(toInt(), true)