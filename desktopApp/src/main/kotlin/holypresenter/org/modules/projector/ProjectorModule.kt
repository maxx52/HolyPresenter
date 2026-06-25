package holypresenter.org.modules.projector

import androidx.compose.runtime.Composable
import holypresenter.org.common.module.HolyModule
import holypresenter.org.common.module.ModuleContext
import holypresenter.org.modules.projector.commands.ShowTextCommand
import holypresenter.org.modules.projector.ui.ProjectorWorkspace

class ProjectorModule : HolyModule {
    private val projectorManager = ProjectorManager()

    override val id: String = "projector"
    override val name: String = "Проектор"
    override val version: String = "1.0.0"

    override fun onLoad(context: ModuleContext) {
        context.commandBus.register(
            commandName = "projector.showText"
        ) { command: ShowTextCommand ->
            projectorManager.showText(command.text)
        }
    }

    @Composable
    override fun Workspace() {
        ProjectorWorkspace(
            currentText = projectorManager.currentText
        )
    }
}