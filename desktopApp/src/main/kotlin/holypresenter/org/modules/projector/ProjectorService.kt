package holypresenter.org.modules.projector

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import holypresenter.org.platform.api.services.HolyService
import holypresenter.org.modules.projector.model.ProjectorContent

class ProjectorService : HolyService {
    override val id: String = "projector.service"

    var currentContent by mutableStateOf<ProjectorContent>(ProjectorContent.Empty)
        private set

    fun showText(text: String) {
        currentContent = ProjectorContent.Text(text)
    }

    fun showBlackScreen() {
        currentContent = ProjectorContent.BlackScreen
    }

    fun clear() {
        currentContent = ProjectorContent.Empty
    }
}