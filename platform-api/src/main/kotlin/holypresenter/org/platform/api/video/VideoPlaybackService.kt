package holypresenter.org.platform.api.video

interface VideoPlaybackService {
    val state: VideoPlaybackState

    fun play(
        path: String,
        loop: Boolean = true,
        muted: Boolean = true
    )

    fun updateOverlay(
        content: VideoOverlayContent
    )

    fun pause()
    fun resume()
    fun stop()
    fun release()
}
