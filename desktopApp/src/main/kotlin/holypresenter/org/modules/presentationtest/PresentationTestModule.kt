package holypresenter.org.modules.presentationtest

import androidx.compose.runtime.Composable
import holypresenter.org.modules.presentationtest.ui.PresentationTestWorkspace
import holypresenter.org.platform.api.module.HolyModule
import holypresenter.org.platform.api.module.ModuleContext
import holypresenter.org.platform.api.module.ModuleMetadata

class PresentationTestModule : HolyModule {

    private lateinit var context: ModuleContext

    override val metadata = ModuleMetadata(
        id = "presentation-test",
        name = "Presentation Test",
        version = "1.0.0",
        apiVersion = "0.1.0",
        author = "HolyPresenter",
        description = "Developer module for testing Presentation API"
    )

    override fun onLoad(context: ModuleContext) {
        this.context = context
    }

    @Composable
    override fun Workspace() {
        PresentationTestWorkspace(
            context = context
        )
    }
}