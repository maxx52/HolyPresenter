package holypresenter.org.modules.presentationtest.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import holypresenter.org.platform.api.module.ModuleContext

@Composable
fun PresentationTestWorkspace(
    context: ModuleContext
) {
    Column {
        Text("Presentation Test")

        Button(
            onClick = {
                println("Show test presentation")
            }
        ) {
            Text("Показать тестовую презентацию")
        }
    }
}