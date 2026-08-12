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
import holypresenter.org.platform.api.audio.AudioPlaybackService
import holypresenter.org.platform.api.planner.PlannerItem
import holypresenter.org.platform.api.planner.PlannerReference
import holypresenter.org.platform.api.planner.PlannerService
import holypresenter.org.modules.quickoutput.QuickOutputState
import holypresenter.org.modules.quickoutput.QuickOutputStateCodec
import kotlinx.coroutines.delay
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun QuickOutputWorkspace(context: ModuleContext) {
    val projector = remember(context) { context.services.get(ProjectionService::class) }
    val audio = remember(context) { context.services.get(AudioPlaybackService::class) }
    val planner = remember(context) { context.services.get(PlannerService::class) }
    var text by remember { mutableStateOf("") }
    var mediaPath by remember { mutableStateOf<String?>(null) }
    var mediaType by remember { mutableStateOf(PresentationBackgroundType.COLOR) }
    var audioPath by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(audio?.currentPath) { while (true) { delay(500); tick++ } }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Быстрый вывод", style = MaterialTheme.typography.headlineMedium)
        Text("Срочно покажите текст, изображение или видео на экране.")
        OutlinedTextField(text, { text = it }, label = { Text("Текст") }, modifier = Modifier.fillMaxWidth().weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { choose("Изображение", "jpg", "jpeg", "png", "webp")?.let { mediaPath = it; mediaType = PresentationBackgroundType.IMAGE } }) { Text("Выбрать картинку") }
            OutlinedButton(onClick = { choose("Видео", "mp4", "mov", "mkv")?.let { mediaPath = it; mediaType = PresentationBackgroundType.VIDEO } }) { Text("Выбрать видео") }
            mediaPath?.let { Text(File(it).name, modifier = Modifier.padding(top = 12.dp)) }
        }
        HorizontalDivider()
        Text("Музыкальный трек", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { choose("Музыкальный трек", "mp3", "wav", "m4a", "aac", "flac", "ogg")?.let { audioPath = it } }) { Text("Выбрать трек") }
            audioPath?.let { Text(File(it).name, modifier = Modifier.padding(top = 12.dp)) }
        }
        val isCurrentTrack = audioPath != null && audioPath == audio?.currentPath
        val canResume = isCurrentTrack && audio?.isPlaying == false
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(enabled = audioPath != null && audio != null, onClick = {
                audioPath?.let { path ->
                    if (canResume) audio?.resume() else audio?.play(path)
                }
            }) { Text(if (canResume) "▶ Продолжить" else "▶ Play") }
            OutlinedButton(enabled = audio?.isPlaying == true, onClick = { audio?.pause() }) { Text("❚❚ Pause") }
            OutlinedButton(enabled = audio?.currentPath != null, onClick = { audio?.stop() }) { Text("■ Stop") }
        }
        val position = remember(tick) { audio?.positionMs ?: 0 }
        val duration = remember(tick) { audio?.durationMs ?: 0 }
        Text("${formatTime(position)} / ${formatTime(duration)}")
        audio?.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(enabled = projector != null, onClick = { projector?.show(quickContent(text, mediaPath, mediaType)) }) { Text("Показать сейчас") }
            OutlinedButton(enabled = planner != null, onClick = {
                val state = QuickOutputState(text, mediaPath, mediaType, audioPath)
                planner?.add(PlannerItem.Generic(PlannerReference("quick-output", QuickOutputStateCodec.encode(state)), text.ifBlank { File(mediaPath ?: audioPath ?: "Быстрый вывод").name }))
            }) { Text("+ В план") }
            OutlinedButton(enabled = projector != null, onClick = { projector?.toggleBlackScreen() }) { Text("Чёрный экран") }
            OutlinedButton(enabled = projector != null, onClick = { projector?.close() }) { Text("Закрыть") }
        }
    }
}

private fun formatTime(value: Long): String { val seconds = value / 1000; return "%02d:%02d".format(seconds / 60, seconds % 60) }

private fun quickContent(
    text: String,
    path: String?,
    type: PresentationBackgroundType
): ProjectionContent.Slide =
    ProjectionContent.Slide(
        presentation =
            Presentation(
                id = "quick-output",
                metadata =
                    PresentationMetadata(
                        title = "Быстрый вывод"
                    ),
                theme =
                    PresentationTheme(
                        background =
                            PresentationBackground(
                                type = type,
                                path = path,
                                color =
                                    if (type == PresentationBackgroundType.COLOR) {
                                        0xFF000000
                                    } else {
                                        null
                                    }
                            ),
                        textStyle =
                            PresentationTextStyle(
                                fontSize = 64,
                                textColor = 0xFFFFFFFF,
                                bold = true
                            ),
                        overlay =
                            PresentationOverlay(
                                enabled =
                                    type !=
                                            PresentationBackgroundType.COLOR
                            )
                    ),
                slides =
                    listOf(
                        PresentationSlide(
                            id = "quick-output-slide",
                            elements =
                                listOf(
                                    TextElement(
                                        id = "quick-output-text",
                                        slot = SlotId("main"),
                                        text = text
                                    )
                                )
                        )
                    )
            ),
        slideIndex = 0
    )

private fun choose(title: String, vararg extensions: String): String? =
    JFileChooser().run {
    dialogTitle = title; isAcceptAllFileFilterUsed = false; fileFilter = FileNameExtensionFilter(title, *extensions)
    if (showOpenDialog(null) == JFileChooser.APPROVE_OPTION) selectedFile.absolutePath else null
}
