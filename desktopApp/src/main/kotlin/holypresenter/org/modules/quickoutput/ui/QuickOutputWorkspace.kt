package holypresenter.org.modules.quickoutput.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import holypresenter.org.platform.api.module.ModuleContext
import holypresenter.org.platform.api.presentation.*
import holypresenter.org.platform.api.presentation.element.TextElement
import holypresenter.org.platform.api.presentation.theme.*
import holypresenter.org.platform.api.projection.ProjectionContent
import holypresenter.org.platform.api.projection.ProjectionService
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun QuickOutputWorkspace(context: ModuleContext) {
    val projector = remember(context) { context.services.get(ProjectionService::class) }
    var text by remember { mutableStateOf("") }
    var mediaPath by remember { mutableStateOf<String?>(null) }
    var mediaType by remember { mutableStateOf(PresentationBackgroundType.COLOR) }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Быстрый вывод", style = MaterialTheme.typography.headlineMedium)
        Text("Срочно покажите текст, изображение или видео на экране.")
        OutlinedTextField(text, { text = it }, label = { Text("Текст") }, modifier = Modifier.fillMaxWidth().weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { choose("Изображение", "jpg", "jpeg", "png", "webp")?.let { mediaPath = it; mediaType = PresentationBackgroundType.IMAGE } }) { Text("Выбрать картинку") }
            OutlinedButton(onClick = { choose("Видео", "mp4", "mov", "mkv")?.let { mediaPath = it; mediaType = PresentationBackgroundType.VIDEO } }) { Text("Выбрать видео") }
            mediaPath?.let { Text(File(it).name, modifier = Modifier.padding(top = 12.dp)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(enabled = projector != null, onClick = { projector?.show(quickContent(text, mediaPath, mediaType)) }) { Text("Показать сейчас") }
            OutlinedButton(enabled = projector != null, onClick = { projector?.toggleBlackScreen() }) { Text("Чёрный экран") }
            OutlinedButton(enabled = projector != null, onClick = { projector?.close() }) { Text("Закрыть") }
        }
    }
}

private fun quickContent(text: String, path: String?, type: PresentationBackgroundType) = ProjectionContent.Slide(
    Presentation("quick-output", PresentationMetadata("Быстрый вывод"), PresentationTheme(PresentationBackground(type, path, if (type == PresentationBackgroundType.COLOR) 0xFF000000 else null), PresentationTextStyle(fontSize = 64, textColor = 0xFFFFFFFF, bold = true), PresentationOverlay(enabled = type != PresentationBackgroundType.COLOR)), listOf(PresentationSlide("quick-output-slide", listOf(TextElement("quick-output-text", SlotId("main"), text))))), 0
)

private fun choose(title: String, vararg extensions: String): String? = JFileChooser().run {
    dialogTitle = title; isAcceptAllFileFilterUsed = false; fileFilter = FileNameExtensionFilter(title, *extensions)
    if (showOpenDialog(null) == JFileChooser.APPROVE_OPTION) selectedFile.absolutePath else null
}
