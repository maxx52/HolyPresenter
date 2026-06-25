package holypresenter.org.modules.projector

import androidx.compose.runtime.Composable
import holypresenter.org.common.module.HolyModule
import holypresenter.org.common.module.ModuleContext
import holypresenter.org.common.module.ModuleMetadata
import holypresenter.org.modules.projector.commands.ShowTextCommand
import holypresenter.org.modules.projector.ui.ProjectorWorkspace

class ProjectorModule : HolyModule {
    private val projectorService = ProjectorService()

    override val metadata = ModuleMetadata(
        id = "projector",
        name = "Проектор",
        version = "1.0.0",
        author = "HolyPresenter",
        description = "Модуль вывода контента на проектор"
    )

    override fun onLoad(context: ModuleContext) {
        context.platform.services.register(
            ProjectorService::class,
            projectorService
        )

        context.platform.commandBus.register(
            commandName = "projector.showText"
        ) { command: ShowTextCommand ->
            projectorService.showText(command.text)
        }
    }

    @Composable
    override fun Workspace() {
        ProjectorWorkspace(
            currentText = projectorService.currentText
        )
    }
}