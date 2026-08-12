package holypresenter.org.platform.api.presentation.theme

data class PresentationTextStyle(
    val fontFamily: String? = null,
    val fontSize: Int = 64,
    /** Shrinks long content to fit its slide frame without clipping. */
    val autoSize: Boolean = true,
    /** Smallest font size allowed when [autoSize] is enabled. */
    val minFontSize: Int = 24,
    val textColor: Long = 0xFFFFFFFF,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val outlineEnabled: Boolean = true,
    val shadowEnabled: Boolean = true
) {
    /**
     * JVM compatibility for modules compiled before auto-sizing was added.
     */
    constructor(
        fontFamily: String?,
        fontSize: Int,
        textColor: Long,
        bold: Boolean,
        italic: Boolean,
        outlineEnabled: Boolean,
        shadowEnabled: Boolean
    ) : this(
        fontFamily = fontFamily,
        fontSize = fontSize,
        autoSize = true,
        minFontSize = 24,
        textColor = textColor,
        bold = bold,
        italic = italic,
        outlineEnabled = outlineEnabled,
        shadowEnabled = shadowEnabled
    )
}
