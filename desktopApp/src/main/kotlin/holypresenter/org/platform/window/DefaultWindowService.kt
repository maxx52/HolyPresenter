package holypresenter.org.platform.window

import androidx.compose.runtime.mutableStateListOf

class DefaultWindowService : WindowService {
    private val _windows = mutableStateListOf<WindowState>()
    override val windows: List<WindowState> = _windows

    override fun register(
        id: String,
        title: String
    ) {
        if (_windows.any { it.id == id }) return

        _windows += WindowState(
            id = id,
            title = title
        )
    }

    override fun open(id: String) {
        update(id) {
            it.copy(isOpen = true)
        }
    }

    override fun close(id: String) {
        update(id) {
            it.copy(isOpen = false)
        }
    }

    override fun isOpen(id: String): Boolean {
        return _windows.firstOrNull { it.id == id }?.isOpen == true
    }

    private fun update(
        id: String,
        transform: (WindowState) -> WindowState
    ) {
        val index = _windows.indexOfFirst { it.id == id }

        if (index >= 0) {
            _windows[index] = transform(_windows[index])
        }
    }
}