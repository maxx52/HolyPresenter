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
import holypresenter.org.platform.api.presentation.Presentation
import holypresenter.org.platform.api.presentation.PresentationMetadata
import holypresenter.org.platform.api.presentation.PresentationSlide
import holypresenter.org.platform.api.presentation.SlotId
import holypresenter.org.platform.api.presentation.element.TextElement
import holypresenter.org.platform.api.presentation.theme.PresentationBackground
import holypresenter.org.platform.api.presentation.theme.PresentationBackgroundType
import holypresenter.org.platform.api.presentation.theme.PresentationOverlay
import holypresenter.org.platform.api.presentation.theme.PresentationTextStyle
import holypresenter.org.platform.api.presentation.theme.PresentationTheme
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

    val testPresentation = remember {
        Presentation(
            id = "test-presentation",
            metadata = PresentationMetadata(
                title = "Тестовая презентация"
            ),
            theme = PresentationTheme(
                background = PresentationBackground(
                    type =
                        PresentationBackgroundType.COLOR,
                    color = 0xFF202124
                ),
                textStyle = PresentationTextStyle(
                    fontSize = 64,
                    textColor = 0xFFFFFFFF,
                    bold = true,
                    outlineEnabled = true,
                    shadowEnabled = true
                ),
                overlay = PresentationOverlay(
                    enabled = false
                )
            ),
            slides = listOf(
                PresentationSlide(
                    id = "test-slide-1",
                    elements = listOf(
                        TextElement(
                            id = "test-text-1",
                            slot = SlotId("main"),
                            text =
                                "HolyPresenter\nработает!"
                        )
                    )
                ),
                PresentationSlide(
                    id = "test-slide-2",
                    elements = listOf(
                        TextElement(
                            id = "test-text-2",
                            slot = SlotId("main"),
                            text =
                                "Второй слайд\nиз Presentation"
                        )
                    )
                )
            )
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
                        presentation =
                            testPresentation,
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
                        presentation =
                            testPresentation,
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
            Text(
                "ProjectionService не зарегистрирован"
            )
        }
    }
}