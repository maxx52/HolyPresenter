package holypresenter.org.platform.api.video

data class VideoOverlayContent(
    val text: String = "",
    val overlayOpacity: Float = 0f,
    val textColor: Long = 0xFFFFFFFF,
    val fontFamily: String? = null,
    val fontSize: Int = 64,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val outlineEnabled: Boolean = true,
    val shadowEnabled: Boolean = true
)
