package holypresenter.org.platform.api.audio

interface AudioPlaybackService {
    val isPlaying: Boolean
    val currentPath: String?
    val positionMs: Long
    val durationMs: Long
    val errorMessage: String?
    fun play(path: String)
    fun pause()
    fun resume()
    fun stop()
    fun release()
}
