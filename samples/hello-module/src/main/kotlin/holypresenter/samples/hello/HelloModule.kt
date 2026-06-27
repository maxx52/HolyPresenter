package holypresenter.samples.hello

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import holypresenter.org.platform.api.module.HolyModule
import holypresenter.org.platform.api.module.ModuleContext
import holypresenter.org.platform.api.module.ModuleMetadata

class HelloModule : HolyModule {
    override val metadata = ModuleMetadata(
        id = "hello",
        name = "Hello Module",
        version = "1.0.0",
        author = "HolyPresenter",
        description = "Sample module for testing Platform API",
        apiVersion = "1.0.0",
    )

    override fun onLoad(context: ModuleContext) {
        println("HelloModule loaded")
    }

    @Composable
    override fun Workspace() {
        Text("Hello from external-style module")
    }
}