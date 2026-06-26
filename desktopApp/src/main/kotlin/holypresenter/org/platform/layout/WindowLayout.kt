package holypresenter.org.platform.layout

data class WindowLayout(
    val id: String,
    val x: Int = 80,
    val y: Int = 0,
    val width: Int = 1200,
    val height: Int = 800,
    val isOpen: Boolean = false
)