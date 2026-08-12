package holypresenter.org.platform.video

import holypresenter.org.platform.api.video.VideoPlaybackStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

class DefaultVideoPlaybackServiceTest {

    @Test
    fun constructor_doesNotStartVlcEngine() {
        var factoryCalls = 0

        DefaultVideoPlaybackService {
            factoryCalls += 1
            error("VLC is unavailable")
        }

        assertEquals(0, factoryCalls)
    }

    @Test
    fun play_keepsApplicationAliveWhenVlcEngineCannotStart() {
        val video = Files.createTempFile("video", ".mp4").toFile()
        val service = DefaultVideoPlaybackService {
            error("VLC is unavailable")
        }

        service.play(video.absolutePath)

        assertEquals(VideoPlaybackStatus.STOPPED, service.state.status)
    }
}
