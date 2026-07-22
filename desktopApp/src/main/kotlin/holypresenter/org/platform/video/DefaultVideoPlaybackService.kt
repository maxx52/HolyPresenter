package holypresenter.org.platform.video

import holypresenter.org.platform.api.video.VideoPlaybackService
import holypresenter.org.platform.api.video.VideoOverlayContent
import holypresenter.org.platform.api.video.VideoPlaybackState
import holypresenter.org.platform.api.video.VideoPlaybackStatus
import java.io.File

class DefaultVideoPlaybackService : VideoPlaybackService {
    private val videoWindow = VideoWindow()
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

        videoWindow.play(
            path = file.absolutePath,
            loop = loop,
            muted = muted
        )

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

        videoWindow.pause()

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

        videoWindow.resume()

        currentState = currentState.copy(
            status = VideoPlaybackStatus.PLAYING
        )
    }

    override fun stop() {
        videoWindow.stop()
        currentState = VideoPlaybackState()
    }

    override fun release() {
        videoWindow.release()
        currentState = VideoPlaybackState()
    }

    override fun updateOverlay(
        content: VideoOverlayContent
    ) {
        videoWindow.updateOverlay(content)
    }
}
