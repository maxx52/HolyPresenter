package holypresenter.org.platform.projection

import holypresenter.org.platform.api.presentation.element.TextElement
import holypresenter.org.platform.api.presentation.theme.PresentationBackgroundType
import holypresenter.org.platform.api.projection.ProjectionContent
import holypresenter.org.platform.api.projection.ProjectionService
import holypresenter.org.platform.api.projection.ProjectionState
import holypresenter.org.platform.api.video.VideoOverlayContent
import holypresenter.org.platform.api.video.VideoPlaybackService
import holypresenter.org.platform.api.video.VideoPlaybackStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class DefaultProjectionService(
    private val videoPlaybackService: VideoPlaybackService
) : ProjectionService {
    private val mutableState = MutableStateFlow(ProjectionState())
    private var contentBehindBlackScreen: ProjectionContent? = null
    override val state: StateFlow<ProjectionState> = mutableState.asStateFlow()
    private val projectionWindow = ProjectionWindow(onClose = ::close)

    override fun show(
        content: ProjectionContent
    ) {
        val currentContent =
            mutableState.value.content
        /*
         * Пока включён чёрный экран, переключение слайдов
         * происходит скрыто. Сам чёрный экран остаётся.
         */
        if (
            currentContent ==
            ProjectionContent.BlackScreen &&
            content != ProjectionContent.BlackScreen
        ) {
            contentBehindBlackScreen = content
            return
        }
        /*
         * Запоминаем контент, который был показан
         * перед включением чёрного экрана.
         */
        if (
            content ==
            ProjectionContent.BlackScreen &&
            currentContent != ProjectionContent.BlackScreen
        ) {
            contentBehindBlackScreen =
                currentContent
        }
        display(content)
    }

    override fun toggleBlackScreen() {
        val currentContent = mutableState.value.content

        if (currentContent == ProjectionContent.BlackScreen) {
            val contentToRestore =
                contentBehindBlackScreen
                    ?: ProjectionContent.Empty

            contentBehindBlackScreen = null
            display(contentToRestore)
        } else {
            contentBehindBlackScreen = currentContent
            display(ProjectionContent.BlackScreen)
        }
    }

    override fun toggleTextVisibility() {
        val currentState = mutableState.value
        val newTextVisible = !currentState.textVisible
        /*
         * Под чёрным экраном только сохраняем состояние.
         * Сам чёрный экран перерисовывать не требуется.
         */
        if (
            currentState.content ==
            ProjectionContent.BlackScreen
        ) {
            mutableState.value =
                currentState.copy(
                    textVisible = newTextVisible
                )
            return
        }

        display(
            content = currentState.content,
            textVisible = newTextVisible
        )
    }

    private fun display(
        content: ProjectionContent,
        textVisible: Boolean = mutableState.value.textVisible
    ) {
        when (content) {
            is ProjectionContent.Slide ->
                showSlide(
                    content = content,
                    textVisible = textVisible
                )
            ProjectionContent.Empty,
            ProjectionContent.BlackScreen,
            ProjectionContent.Logo ->
                showStandardContent(
                    content = content,
                    textVisible = textVisible
                )
        }

        mutableState.value =
            mutableState.value.copy(
                content = content,
                visible = true,
                textVisible = textVisible
            )
    }

    override fun clear() {
        show(ProjectionContent.Empty)
    }

    override fun close() {
        contentBehindBlackScreen = null
        videoPlaybackService.stop()
        projectionWindow.close()
        mutableState.value = ProjectionState(
            content = ProjectionContent.Empty,
            visible = false,
            textVisible = true
        )
    }

    private fun showSlide(
        content: ProjectionContent.Slide,
        textVisible: Boolean
    ) {
        when (
            content.presentation
                .theme
                .background
                .type
        ) {
            PresentationBackgroundType.VIDEO ->
                showVideoSlide(
                    content = content,
                    textVisible = textVisible
                )

            PresentationBackgroundType.COLOR,
            PresentationBackgroundType.IMAGE ->
                showStandardContent(
                    content = content,
                    textVisible = textVisible
                )
        }
    }

    private fun showStandardContent(
        content: ProjectionContent,
        textVisible: Boolean
    ) {
        videoPlaybackService.stop()
        projectionWindow.show(
            content = content,
            textVisible = textVisible
        )
    }

    private fun showVideoSlide(
        content: ProjectionContent.Slide,
        textVisible: Boolean
    ) {
        val path = content.presentation
            .theme
            .background
            .path
            ?.takeIf(String::isNotBlank)

        val videoFile = path
            ?.let(::File)
            ?.takeIf(File::isFile)

        val overlay =
            content.toVideoOverlayContent(
                textVisible = textVisible
            )

        /*
         * Если файл отсутствует, всё равно показываем
         * текст слайда на чёрном фоне вместо пустого экрана.
         */
        if (videoFile == null) {
            println("Video background not found: " + (path ?: "<empty>"))
            showStandardContent(
                content,
                textVisible = false
            )
            return
        }

        projectionWindow.close()

        val absolutePath = videoFile.absolutePath
        val videoState = videoPlaybackService.state

        when {
            /*
             * Видео уже воспроизводится.
             * Меняем только текст — файл не перезапускаем.
             */
            videoState.currentPath == absolutePath &&
                    videoState.status == VideoPlaybackStatus.PLAYING -> {
                videoPlaybackService.updateOverlay(overlay)
            }

            /*
             * То же видео было поставлено на паузу.
             */
            videoState.currentPath == absolutePath &&
                    videoState.status == VideoPlaybackStatus.PAUSED -> {
                videoPlaybackService.updateOverlay(
                    overlay
                )
                videoPlaybackService.resume()
            }

            /*
             * Выбрано другое видео либо проигрыватель
             * ещё не был запущен.
             */
            else -> {
                videoPlaybackService.stop()
                videoPlaybackService.updateOverlay(overlay)

                videoPlaybackService.play(
                    path = absolutePath,
                    loop = true,
                    muted = true
                )
            }
        }
    }
}

private fun ProjectionContent.Slide
        .toVideoOverlayContent(
    textVisible: Boolean
): VideoOverlayContent {
    val theme = presentation.theme

    val text =
        if (textVisible) {
            slide
                ?.elements
                ?.asSequence()
                ?.filter { element ->
                    element.visible
                }
                ?.sortedBy { element ->
                    element.zIndex
                }
                ?.filterIsInstance<TextElement>()
                ?.joinToString("\n") { element ->
                    element.text
                }
                .orEmpty()
        } else {
            ""
        }

    return VideoOverlayContent(
        text = text,
        overlayOpacity =
            if (theme.overlay.enabled) {
                theme.overlay.opacity
            } else {
                0f
            },
        textColor = theme.textStyle.textColor,
        fontFamily = theme.textStyle.fontFamily,
        fontSize = theme.textStyle.fontSize,
        bold = theme.textStyle.bold,
        italic = theme.textStyle.italic,
        outlineEnabled =
            theme.textStyle.outlineEnabled,
        shadowEnabled =
            theme.textStyle.shadowEnabled
    )
}