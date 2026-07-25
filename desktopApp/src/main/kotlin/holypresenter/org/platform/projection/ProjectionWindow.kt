package holypresenter.org.platform.projection

import holypresenter.org.platform.api.projection.ProjectionBackgroundType
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
    private var content: ProjectionContent =
        ProjectionContent.Empty

    private var backgroundImage: BufferedImage? = null

    init {
        background = Color.BLACK
        isOpaque = true
    }

    fun updateContent(
        content: ProjectionContent
    ) {
        this.content = content
        backgroundImage = loadBackgroundImage(content)
        repaint()
    }

    override fun paintComponent(
        graphics: Graphics
    ) {
        super.paintComponent(graphics)

        val graphics2D = graphics.create() as Graphics2D

        try {
            graphics2D.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            )

            graphics2D.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            )

            graphics2D.color = Color.BLACK
            graphics2D.fillRect(
                0,
                0,
                width,
                height
            )

            when (val current = content) {
                ProjectionContent.Empty -> Unit

                is ProjectionContent.Slide -> {
                    drawBackground(graphics2D)
                    drawText(
                        graphics = graphics2D,
                        text = current.text
                    )
                }
                else -> {}
            }
        } finally {
            graphics2D.dispose()
        }
    }

    private fun drawBackground(
        graphics: Graphics2D
    ) {
        val image = backgroundImage ?: return

        val scale = max(
            width.toDouble() / image.width,
            height.toDouble() / image.height
        )

        val targetWidth =
            (image.width * scale).toInt()

        val targetHeight =
            (image.height * scale).toInt()

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

    private fun drawText(
        graphics: Graphics2D,
        text: String
    ) {
        if (text.isBlank()) return

        graphics.font = Font(
            Font.SANS_SERIF,
            Font.BOLD,
            64
        )

        val lines = text.lines()
        val metrics = graphics.fontMetrics
        val lineHeight = metrics.height
        val totalHeight = lines.size * lineHeight

        var y =
            (height - totalHeight) / 2 +
                    metrics.ascent

        lines.forEach { line ->
            val x =
                (width - metrics.stringWidth(line)) / 2

            drawTextOutline(
                graphics = graphics,
                text = line,
                x = x,
                y = y
            )

            graphics.color = Color.WHITE
            graphics.drawString(
                line,
                x,
                y
            )

            y += lineHeight
        }
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
                if (offsetX == 0 && offsetY == 0) {
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
        val slide =
            content as? ProjectionContent.Slide
                ?: return null

        if (
            slide.backgroundType !=
            ProjectionBackgroundType.IMAGE
        ) {
            return null
        }

        val path =
            slide.backgroundPath
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