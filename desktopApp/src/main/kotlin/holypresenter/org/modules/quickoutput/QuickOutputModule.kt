package holypresenter.org.modules.quickoutput

import androidx.compose.runtime.Composable
import holypresenter.org.modules.quickoutput.ui.QuickOutputWorkspace
import holypresenter.org.platform.api.module.HolyModule
import holypresenter.org.platform.api.module.ModuleContext
import holypresenter.org.platform.api.module.ModuleMetadata

class QuickOutputModule : HolyModule {
    private lateinit var context: ModuleContext

    override val metadata = ModuleMetadata(
        id = "quick-output",
        name = "Быстрый вывод",
        version = "1.0.0",
        apiVersion = "0.6.0",
        author = "HolyPresenter",
        description = "Urgent text, image and video projection",
        icon = "⚡"
    )

    override fun onLoad(context: ModuleContext) { this.context = context }

    @Composable
    override fun Workspace() { QuickOutputWorkspace(context) }
}
