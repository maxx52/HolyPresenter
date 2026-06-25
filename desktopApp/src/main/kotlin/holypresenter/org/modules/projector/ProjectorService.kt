package holypresenter.org.modules.projector

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import holypresenter.org.common.services.HolyService

class ProjectorService : HolyService {
    override val id: String = "projector.service"

    var currentText by mutableStateOf("Проектор готов")
        private set

    fun showText(text: String) {
        currentText = text
    }

    fun clear() {
        currentText = ""
    }
}