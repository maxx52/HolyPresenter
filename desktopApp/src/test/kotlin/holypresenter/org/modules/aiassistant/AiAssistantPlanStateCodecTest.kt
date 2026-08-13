package holypresenter.org.modules.aiassistant

import holypresenter.org.platform.api.presentation.theme.PresentationBackgroundType
import kotlin.test.Test
import kotlin.test.assertEquals

class AiAssistantPlanStateCodecTest {
    @Test
    fun codec_roundTripsUnicodeAndMediaPath() {
        val state = AiAssistantPlanState(
            text = "Молодёжное служение\nСегодня",
            mediaPath = "C:\\Изображения\\фон.jpg",
            mediaType = PresentationBackgroundType.IMAGE
        )

        assertEquals(state, AiAssistantPlanStateCodec.decode(AiAssistantPlanStateCodec.encode(state)))
    }
}
