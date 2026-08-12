package holypresenter.org.platform.video

import holypresenter.org.platform.api.video.VideoPlaybackService
import holypresenter.org.platform.api.video.VideoOverlayContent
import holypresenter.org.platform.api.video.VideoPlaybackState
import holypresenter.org.platform.api.video.VideoPlaybackStatus
import java.io.File

class DefaultVideoPlaybackService(
    private val videoWindowFactory: () -> VideoWindow = ::VideoWindow
) : VideoPlaybackService {
    private var videoWindow: VideoWindow? = null
    private var pendingOverlay: VideoOverlayContent? = null
    private var currentState = VideoPlaybackState()

    override val state: VideoPlaybackState
        get() = currentState

    override fun play(
        path: String,
        loop: Boolean,
        muted: Boolean
    ) {
        val file = File(path)

        if (!file.isFile) {
            println("Video file not found: $path")
            return
        }

        val window = getOrCreateVideoWindow() ?: return

        val started = runCatching {
            pendingOverlay?.let(window::updateOverlay)
            window.play(
                path = file.absolutePath,
                loop = loop,
                muted = muted
            )
        }.onFailure { error ->
            println("Video playback unavailable: ${error.message}")
        }.isSuccess

        if (!started) return

        currentState = VideoPlaybackState(
            status = VideoPlaybackStatus.PLAYING,
            currentPath = file.absolutePath,
            loop = loop,
            muted = muted
        )
    }

    override fun pause() {
        if (
            currentState.status !=
            VideoPlaybackStatus.PLAYING
        ) {
            return
        }

        val window = videoWindow ?: return
        window.pause()

        currentState = currentState.copy(
            status = VideoPlaybackStatus.PAUSED
        )
    }

    override fun resume() {
        if (
            currentState.status !=
            VideoPlaybackStatus.PAUSED
        ) {
            return
        }

        val window = videoWindow ?: return
        window.resume()

        currentState = currentState.copy(
            status = VideoPlaybackStatus.PLAYING
        )
    }

    override fun stop() {
        if (currentState.status == VideoPlaybackStatus.STOPPED) {
            return
        }
        videoWindow?.stop()
        currentState = VideoPlaybackState()
    }

    override fun release() {
        videoWindow?.let { window ->
            runCatching(window::release)
        }
        videoWindow = null
        pendingOverlay = null
        currentState = VideoPlaybackState()
    }

    override fun updateOverlay(
        content: VideoOverlayContent
    ) {
        pendingOverlay = content
        videoWindow?.updateOverlay(content)
    }

    private fun getOrCreateVideoWindow(): VideoWindow? {
        videoWindow?.let { return it }

        return runCatching(videoWindowFactory)
            .onFailure { error ->
                println("VLC video engine is unavailable: ${error.message}")
            }
            .getOrNull()
            ?.also { videoWindow = it }
    }
}
