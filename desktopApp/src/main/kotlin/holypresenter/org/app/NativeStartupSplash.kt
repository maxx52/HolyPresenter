package holypresenter.org.app

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.EventQueue
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.JWindow

internal class NativeStartupSplash private constructor() : JWindow() {
    private val artwork: BufferedImage = requireNotNull(
        NativeStartupSplash::class.java.getResourceAsStream("/holypresenter-splash.jpg")
    ) { "Не найден ресурс заставки HolyPresenter" }.use(ImageIO::read)

    init {
        contentPane = object : javax.swing.JPanel() {
            override fun paintComponent(graphics: Graphics) {
                val canvas = graphics.create() as Graphics2D
                try {
                    canvas.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC
                    )
                    canvas.drawImage(artwork, 0, 0, width, height, null)
                    canvas.composite = AlphaComposite.SrcOver.derive(0.18f)
                    canvas.color = Color(7, 10, 34)
                    canvas.fillRect(0, 0, width, height)
                    canvas.composite = AlphaComposite.SrcOver
                    canvas.setRenderingHint(
                        RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                    )

                    canvas.color = Color.WHITE
                    canvas.font = Font("Dialog", Font.BOLD, 42)
                    val title = "HolyPresenter"
                    val titleWidth = canvas.fontMetrics.stringWidth(title)
                    canvas.drawString(title, (width - titleWidth) / 2, height / 2)

                    canvas.font = Font("Dialog", Font.PLAIN, 16)
                    canvas.color = Color(234, 218, 255)
                    canvas.drawString("Загрузка HolyPresenter и модулей…", 36, height - 58)
                    canvas.font = Font("Dialog", Font.PLAIN, 13)
                    canvas.color = Color(202, 188, 220)
                    canvas.drawString("Версия 1.0.4", 36, height - 30)
                } finally {
                    canvas.dispose()
                }
            }
        }
        setSize(700, 440)
        setLocationRelativeTo(null)
        isAlwaysOnTop = true
    }

    fun close() {
        EventQueue.invokeLater {
            isVisible = false
            dispose()
        }
    }

    companion object {
        fun show(): NativeStartupSplash = NativeStartupSplash().also { splash ->
            EventQueue.invokeLater {
                splash.isVisible = true
                splash.toFront()
            }
        }
    }
}
