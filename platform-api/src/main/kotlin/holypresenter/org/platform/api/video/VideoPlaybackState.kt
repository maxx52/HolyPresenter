package holypresenter.org.platform.api.video

data class VideoPlaybackState(
    val status: VideoPlaybackStatus = VideoPlaybackStatus.STOPPED,
    val currentPath: String? = null,
    val loop: Boolean = true,
    val muted: Boolean = true
) {
    val isPlaying: Boolean
        get() = status == VideoPlaybackStatus.PLAYING
}