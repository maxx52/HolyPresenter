package holypresenter.org.platform.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class VlcAudioPlaybackServiceTest {

    @Test
    fun constructor_doesNotStartVlcEngine() {
        val service = VlcAudioPlaybackService()

        assertFalse(service.isPlaying)
        assertNull(service.currentPath)
        assertNull(service.errorMessage)
    }
}
