package holypresenter.org.modules.update

import androidx.compose.runtime.Composable
import holypresenter.org.modules.update.ui.UpdateWorkspace
import holypresenter.org.platform.api.module.HolyModule
import holypresenter.org.platform.api.module.ModuleMetadata
import holypresenter.org.platform.update.ApplicationUpdateService

class UpdateModule(
    private val updateService: ApplicationUpdateService
) : HolyModule {
    override val metadata = ModuleMetadata(
        id = "updates",
        name = "Обновления",
        version = "1.0.0",
        apiVersion = "1.0.0",
        author = "HolyPresenter",
        description = "Проверка и безопасная установка обновлений HolyPresenter",
        icon = "🚀"
    )

    @Composable
    override fun Workspace() {
        UpdateWorkspace(updateService)
    }
}
