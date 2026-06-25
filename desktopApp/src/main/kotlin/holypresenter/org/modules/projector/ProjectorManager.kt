package holypresenter.org.modules.projector

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ProjectorManager {
    var currentText by mutableStateOf("Проектор готов")
        private set

    fun showText(text: String) {
        currentText = text
    }
}