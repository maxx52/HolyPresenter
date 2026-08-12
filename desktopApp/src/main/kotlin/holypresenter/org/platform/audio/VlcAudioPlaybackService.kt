package holypresenter.org.platform.audio

import holypresenter.org.platform.api.audio.AudioPlaybackService
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import java.io.File

class VlcAudioPlaybackService : AudioPlaybackService {
    private val factory = MediaPlayerFactory()
    private val player = factory.mediaPlayers().newMediaPlayer()
    override var currentPath: String? = null
        private set
    override var isPlaying: Boolean = false
        private set

    override fun play(path: String) {
        val file = File(path)
        if (!file.isFile) return
        player.media().play(file.absolutePath, ":no-video", ":no-video-title-show")
        currentPath = file.absolutePath
        isPlaying = true
    }
    override fun pause() { if (isPlaying) { player.controls().pause(); isPlaying = false } }
    override fun resume() { if (currentPath != null) { player.controls().play(); isPlaying = true } }
    override fun stop() { player.controls().stop(); currentPath = null; isPlaying = false }
    override fun release() { player.release(); factory.release(); currentPath = null; isPlaying = false }
}
