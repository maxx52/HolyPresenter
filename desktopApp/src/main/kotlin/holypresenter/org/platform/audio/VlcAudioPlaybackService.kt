package holypresenter.org.platform.audio

import holypresenter.org.platform.api.audio.AudioPlaybackService
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import java.io.File
import kotlin.runCatching

class VlcAudioPlaybackService : AudioPlaybackService {
    private data class Engine(
        val factory: MediaPlayerFactory,
        val player: MediaPlayer
    )

    private var engineInitializationAttempted = false
    private var engine: Engine? = null
    private var playbackError: String? = null

    override var currentPath: String? = null
        private set
    override var isPlaying: Boolean = false
        private set
    override var positionMs: Long = 0
        get() = engine?.player?.status()?.time() ?: field
    override var durationMs: Long = 0
        get() = engine?.player?.status()?.length() ?: field
    override val errorMessage: String?
        get() = playbackError

    override fun play(path: String) {
        val file = File(path)
        if (!file.isFile) {
            playbackError = "Файл трека не найден"
            return
        }

        val mediaPlayer = getOrCreateEngine()?.player ?: return
        playbackError = null

        runCatching {
            // Явно включаем звук: настройки VLC могли остаться выключенными после видео.
            mediaPlayer.audio().isMute = false
            mediaPlayer.audio().setVolume(100)
            mediaPlayer.media().play(
                file.absolutePath,
                ":no-video",
                ":no-video-title-show"
            )
        }
            .onFailure { error ->
                playbackError = "Не удалось открыть трек: ${error.message}"
            }
        currentPath = file.absolutePath
        isPlaying = playbackError == null
    }

    override fun pause() {
        if (isPlaying) {
            engine?.player?.controls()?.pause()
            isPlaying = false
        }
    }

    override fun resume() {
        if (currentPath != null) {
            engine?.player?.controls()?.play()
            isPlaying = engine != null
        }
    }

    override fun stop() {
        engine?.player?.controls()?.stop()
        currentPath = null
        isPlaying = false
        positionMs = 0
    }

    override fun release() {
        engine?.let { activeEngine ->
            runCatching { activeEngine.player.release() }
            runCatching { activeEngine.factory.release() }
        }
        engine = null
        currentPath = null
        isPlaying = false
    }

    private fun getOrCreateEngine(): Engine? {
        engine?.let { return it }
        if (engineInitializationAttempted) return null
        engineInitializationAttempted = true

        return runCatching {
            val factory = MediaPlayerFactory()
            runCatching {
                Engine(
                    factory = factory,
                    player = factory.mediaPlayers().newMediaPlayer()
                )
            }.getOrElse { error ->
                runCatching { factory.release() }
                throw error
            }
        }.onFailure { error ->
            playbackError =
                "VLC недоступен: ${error.message ?: error::class.java.simpleName}"
            println("VLC audio engine is unavailable: ${error.message}")
        }.getOrNull()?.also { createdEngine ->
            engine = createdEngine
        }
    }
}
