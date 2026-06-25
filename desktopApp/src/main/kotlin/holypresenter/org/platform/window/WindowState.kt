package holypresenter.org.platform.window

data class WindowState(
    val id: String,
    val title: String,
    val isOpen: Boolean = false
)