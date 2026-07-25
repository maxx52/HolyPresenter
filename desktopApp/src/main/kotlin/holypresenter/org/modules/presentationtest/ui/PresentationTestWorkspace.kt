package holypresenter.org.modules.presentationtest.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import holypresenter.org.platform.api.module.ModuleContext
import holypresenter.org.platform.api.projection.ProjectionBackgroundType
import holypresenter.org.platform.api.projection.ProjectionContent
import holypresenter.org.platform.api.projection.ProjectionService

@Composable
fun PresentationTestWorkspace(
    context: ModuleContext
) {
    val projectionService = remember(context) {
        context.services.get(
            ProjectionService::class
        )
    }

    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        Text("Presentation Test")

        Button(
            enabled = projectionService != null,
            onClick = {
                projectionService?.show(
                    ProjectionContent.Slide(
                        presentationId =
                            "test-presentation",
                        slideId =
                            "test-slide",
                        text =
                            "HolyPresenter\nработает!",
                        backgroundType =
                            ProjectionBackgroundType.NONE
                    )
                )
            }
        ) {
            Text("Показать тестовый слайд")
        }

        OutlinedButton(
            enabled = projectionService != null,
            onClick = {
                projectionService?.clear()
            }
        ) {
            Text("Очистить экран")
        }

        OutlinedButton(
            enabled = projectionService != null,
            onClick = {
                projectionService?.close()
            }
        ) {
            Text("Закрыть проектор")
        }

        if (projectionService == null) {
            Text("ProjectionService не зарегистрирован")
        }
    }
}