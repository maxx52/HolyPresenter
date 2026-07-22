package holypresenter.org.platform.video

import holypresenter.org.platform.api.video.VideoOverlayContent
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import java.awt.BorderLayout
import java.awt.Color
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.WindowConstants

class VideoWindow {
    private val mediaPlayerComponent = EmbeddedMediaPlayerComponent()
    fun currentBounds(): Rectangle = frame.bounds

    private val frame = JFrame().apply {
        title = "HolyPresenter Video Test"
        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        contentPane.layout = BorderLayout()
        background = Color.BLACK

        contentPane.add(
            mediaPlayerComponent,
            BorderLayout.CENTER
        )

        rootPane.registerKeyboardAction(
            {
                stop()
            },
            KeyStroke.getKeyStroke("ESCAPE"),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        )

        addWindowListener(
            object : WindowAdapter() {
                override fun windowClosing(event: WindowEvent) {
                    stop()
                }
            }
        )
    }

    private val overlayWindow =
        VideoOverlayWindow(
            owner = frame,
            onEscape = {
                stop()
            }
        )

    init {
        mediaPlayerComponent
            .mediaPlayer()
            .overlay()
            .set(overlayWindow)
    }

    private fun showOnProjector() {
        val devices = GraphicsEnvironment
            .getLocalGraphicsEnvironment()
            .screenDevices

        if (devices.size > 1) {
            val bounds = devices[1]
                .defaultConfiguration
                .bounds

            frame.dispose()
            frame.isUndecorated = true
            frame.isAlwaysOnTop = true
            frame.bounds = bounds
        } else {
            frame.dispose()
            frame.isUndecorated = false
            frame.isAlwaysOnTop = false
            frame.setSize(960, 540)
            frame.setLocationRelativeTo(null)
        }
        frame.isVisible = true
        frame.validate()
        frame.toFront()
    }

    fun play(
        path: String,
        loop: Boolean,
        muted: Boolean
    ) {
        runOnSwingThread {
            showOnProjector()

            val player = mediaPlayerComponent.mediaPlayer()

            player.audio().isMute = muted

            val options = buildList {
                add(":no-video-title-show")
                if (loop) {
                    add(":input-repeat=65535")
                }
            }

            player.media().play(
                path,
                *options.toTypedArray()
            )

            enableOverlayWhenReady()
        }
    }

    fun pause() {
        runOnSwingThread {
            mediaPlayerComponent
                .mediaPlayer()
                .controls()
                .pause()
        }
    }

    fun resume() {
        runOnSwingThread {
            mediaPlayerComponent
                .mediaPlayer()
                .controls()
                .play()
        }
    }

    fun stop() {
        runOnSwingThread {
            val player = mediaPlayerComponent.mediaPlayer()
            runCatching {
                player.overlay().enable(false)
            }
            player.controls().stop()
            frame.isVisible = false
        }
    }

    fun release() {
        runOnSwingThread {
            mediaPlayerComponent
                .mediaPlayer()
                .overlay()
                .enable(false)

            overlayWindow.dispose()

            mediaPlayerComponent
                .mediaPlayer()
                .controls()
                .stop()

            mediaPlayerComponent.release()
            frame.dispose()
        }
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

    fun updateOverlay(
        content: VideoOverlayContent
    ) {
        runOnSwingThread {
            overlayWindow.updateContent(content)
        }
    }

    private fun enableOverlayWhenReady() {
        val player = mediaPlayerComponent.mediaPlayer()

        SwingUtilities.invokeLater {
            if (
                frame.isShowing &&
                mediaPlayerComponent.isShowing
            ) {
                player.overlay().enable(true)
            } else {
                Timer(50) {
                    if (
                        frame.isShowing &&
                        mediaPlayerComponent.isShowing
                    ) {
                        player.overlay().enable(true)
                        (it.source as Timer).stop()
                    }
                }.apply {
                    isRepeats = true
                    start()
                }
            }
        }
    }
}
