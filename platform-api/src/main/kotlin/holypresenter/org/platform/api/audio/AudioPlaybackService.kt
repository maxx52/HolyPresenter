package holypresenter.org.platform.api.audio

interface AudioPlaybackService {
    val isPlaying: Boolean
    val currentPath: String?
    fun play(path: String)
    fun pause()
    fun resume()
    fun stop()
    fun release()
}
