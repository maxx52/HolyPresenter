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
import holypresenter.org.modules.presentationtest.PresentationTestContent
import holypresenter.org.platform.api.module.ModuleContext
import holypresenter.org.platform.api.planner.PlannerItem
import holypresenter.org.platform.api.planner.PlannerReference
import holypresenter.org.platform.api.planner.PlannerService
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

    val plannerService = remember(context) {
        context.services.get(
            PlannerService::class
        )
    }

    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Presentation Test")

        Button(
            enabled = plannerService != null,
            onClick = {
                plannerService?.add(
                    PlannerItem.Generic(
                        reference = PlannerReference(
                            moduleId = "presentation-test",
                            itemId = PresentationTestContent.ITEM_ID
                        ),
                        title = PresentationTestContent.TITLE
                    )
                )
            }
        ) {
            Text("Добавить в план")
        }

        Button(
            enabled = projectionService != null,
            onClick = {
                projectionService?.show(
                    ProjectionContent.Slide(
                        presentation = PresentationTestContent.presentation,
                        slideIndex = 0
                    )
                )
            }
        ) {
            Text("Показать первый слайд")
        }

        Button(
            enabled = projectionService != null,
            onClick = {
                projectionService?.show(
                    ProjectionContent.Slide(
                        presentation = PresentationTestContent.presentation,
                        slideIndex = 1
                    )
                )
            }
        ) {
            Text("Показать второй слайд")
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