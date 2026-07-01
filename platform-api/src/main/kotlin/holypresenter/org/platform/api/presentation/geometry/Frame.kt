package holypresenter.org.platform.api.presentation.geometry

data class Frame(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
) {
    companion object {
        val Fullscreen = Frame(
            x = 0f,
            y = 0f,
            width = 1f,
            height = 1f
        )
    }
}