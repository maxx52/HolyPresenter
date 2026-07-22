package holypresenter.org.platform.video

import holypresenter.org.platform.api.video.VideoOverlayContent
import uk.co.caprica.vlcj.player.component.overlay.AbstractJWindowOverlayComponent
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Window
import javax.swing.JComponent
import javax.swing.KeyStroke

class VideoOverlayWindow(
    owner: Window,
    private val onEscape: () -> Unit
) : AbstractJWindowOverlayComponent(owner) {
    @Volatile
    private var content = VideoOverlayContent()

    fun updateContent(
        content: VideoOverlayContent
    ) {
        this.content = content
        repaint()
    }

    override fun onCreateOverlay() {
        rootPane.registerKeyboardAction(
            { onEscape() },
            KeyStroke.getKeyStroke("ESCAPE"),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        )
    }

    override fun onNewSize(width: Int, height: Int) = Unit

    override fun onPaintOverlay(
        graphics: Graphics2D
    ) {
        val current = content

        graphics.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        )
        graphics.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        )

        drawDarkOverlay(
            graphics = graphics,
            opacity = current.overlayOpacity
        )

        drawCenteredText(
            graphics = graphics,
            content = current
        )
    }

    private fun drawDarkOverlay(
        graphics: Graphics2D,
        opacity: Float
    ) {
        val alpha = (opacity.coerceIn(0f, 1f) * 255)
            .toInt()

        if (alpha == 0) return

        graphics.color = Color(0, 0, 0, alpha)
        graphics.fillRect(0, 0, width, height)
    }

    private fun drawCenteredText(
        graphics: Graphics2D,
        content: VideoOverlayContent
    ) {
        if (content.text.isBlank()) return

        val fontStyle = when {
            content.bold && content.italic ->
                Font.BOLD or Font.ITALIC

            content.bold ->
                Font.BOLD

            content.italic ->
                Font.ITALIC

            else ->
                Font.PLAIN
        }

        graphics.font = Font(
            Font.SANS_SERIF,
            fontStyle,
            content.fontSize.coerceAtLeast(24)
        )

        val lines = content.text.lines()
        val metrics = graphics.fontMetrics
        val lineHeight = metrics.height
        val totalHeight = lines.size * lineHeight

        var y = (height - totalHeight) / 2 + metrics.ascent

        val textColor = Color(
            content.textColor.toInt(),
            true
        )

        lines.forEach { line ->
            val x = (width - metrics.stringWidth(line)) / 2

            if (content.outlineEnabled) {
                drawOutline(
                    graphics = graphics,
                    text = line,
                    x = x,
                    y = y
                )
            }

            graphics.color = textColor
            graphics.drawString(line, x, y)

            y += lineHeight
        }
    }

    private fun drawOutline(
        graphics: Graphics2D,
        text: String,
        x: Int,
        y: Int
    ) {
        graphics.color = Color.BLACK

        val offsets = listOf(
            -2 to -2,
            0 to -2,
            2 to -2,
            -2 to 0,
            2 to 0,
            -2 to 2,
            0 to 2,
            2 to 2
        )

        offsets.forEach { (dx, dy) ->
            graphics.drawString(
                text,
                x + dx,
                y + dy
            )
        }
    }

    private fun fontStyle(): Int =
        when {
            content.bold && content.italic -> Font.BOLD or Font.ITALIC
            content.bold -> Font.BOLD
            content.italic -> Font.ITALIC
            else -> Font.PLAIN
        }
}
