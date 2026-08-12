package holypresenter.org.platform.audio

import holypresenter.org.platform.api.audio.AudioPlaybackService
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import java.io.File
import kotlin.runCatching

class VlcAudioPlaybackService : AudioPlaybackService {
    private val factory = runCatching { MediaPlayerFactory() }.getOrNull()
    private val player = factory?.mediaPlayers()?.newMediaPlayer()
    override var currentPath: String? = null
        private set
    override var isPlaying: Boolean = false
        private set
    override var positionMs: Long = 0
        get() = player?.status()?.time() ?: field
    override var durationMs: Long = 0
        get() = player?.status()?.length() ?: field
    override var errorMessage: String? =
        if (player == null) "VLC не найден. Установите VLC Media Player." else null
        private set

    override fun play(path: String) {
        val file = File(path)
        if (!file.isFile) { errorMessage = "Файл трека не найден"; return }
        val mediaPlayer = player ?: return
        runCatching { mediaPlayer.media().play(file.absolutePath, ":no-video", ":no-video-title-show") }
            .onFailure { errorMessage = "Не удалось открыть трек: ${it.message}" }
        currentPath = file.absolutePath
        isPlaying = errorMessage == null
    }
    override fun pause() { if (isPlaying) { player?.controls()?.pause(); isPlaying = false } }
    override fun resume() { if (currentPath != null) { player?.controls()?.play(); isPlaying = true } }
    override fun stop() { player?.controls()?.stop(); currentPath = null; isPlaying = false; positionMs = 0 }
    override fun release() { player?.release(); factory?.release(); currentPath = null; isPlaying = false }
}
