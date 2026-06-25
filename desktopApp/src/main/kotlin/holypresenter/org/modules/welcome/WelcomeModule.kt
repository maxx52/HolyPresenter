package holypresenter.org.modules.welcome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import holypresenter.org.platform.api.docking.DockPanel
import holypresenter.org.platform.api.docking.DockPosition
import holypresenter.org.platform.api.module.HolyModule
import holypresenter.org.platform.api.module.ModuleContext
import holypresenter.org.platform.api.module.ModuleMetadata
import holypresenter.org.modules.projector.commands.ShowTextCommand
import holypresenter.org.modules.welcome.commands.ShowWelcomeMessageCommand
import holypresenter.org.modules.welcome.events.WelcomeClickedEvent
import holypresenter.org.modules.welcome.ui.WelcomeInfoPanel

class WelcomeModule : HolyModule {
    override val metadata = ModuleMetadata(
        id = "welcome",
        name = "Добро пожаловать",
        version = "1.0.0",
        author = "HolyPresenter",
        description = "Тестовый модуль приветствия"
    )

    private lateinit var context: ModuleContext

    @Composable
    override fun Workspace() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Модуль Welcome работает")
        }

        Button(
            onClick = {
                context.platform.eventBus.publish(
                    WelcomeClickedEvent("Кнопка Welcome нажата")
                )
            }
        ) {
            Text("Отправить событие")
        }

        Button(
            onClick = {
                context.platform.commandBus.execute(
                    ShowWelcomeMessageCommand("Привет из CommandBus")
                )
            }
        ) {
            Text("Выполнить команду")
        }

        Button(
            onClick = {
                context.platform.commandBus.execute(
                    ShowTextCommand("Привет на проектор из WelcomeModule")
                )
            }
        ) {
            Text("Показать текст на проекторе")
        }
    }

    override fun dockPanels() = listOf(
        DockPanel(
            id = "welcome.info",
            title = "Информация",
            position = DockPosition.RIGHT,
            content = WelcomeInfoPanel()
        )
    )

    override fun onLoad(context: ModuleContext) {
        this.context = context

        context.platform.eventBus.subscribe("welcome.clicked") { event ->
            println("Получено событие: ${event.name}")
        }

        context.platform.commandBus.register(
            commandName = "welcome.showMessage"
        ) { command: ShowWelcomeMessageCommand ->
            println("Команда получена: ${command.message}")
        }
    }
}