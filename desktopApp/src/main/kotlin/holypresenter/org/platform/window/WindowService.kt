package holypresenter.org.platform.window

interface WindowService {
    val windows: List<WindowState>

    fun register(
        id: String,
        title: String
    )

    fun open(id: String)

    fun close(id: String)

    fun isOpen(id: String): Boolean
}