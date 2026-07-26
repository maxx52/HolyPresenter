package holypresenter.org.platform.window

import java.awt.GraphicsEnvironment
import javax.swing.JFrame

private const val DEFAULT_PROJECTOR_INDEX = 1
private const val PREVIEW_WIDTH = 960
private const val PREVIEW_HEIGHT = 540

/**
 * Настраивает и показывает Swing-окно:
 *
 * - на втором мониторе — без рамки, на весь экран;
 * - при одном мониторе — как обычное тестовое окно;
 * - проектор не забирает фокус у панели оператора.
 */
internal fun JFrame.showOnProjectorScreen(
    projectorIndex: Int = DEFAULT_PROJECTOR_INDEX
) {
    val devices = GraphicsEnvironment
        .getLocalGraphicsEnvironment()
        .screenDevices

    val projectorDevice =
        devices.getOrNull(projectorIndex)

    /*
     * Изменять isUndecorated у уже отображаемого
     * JFrame можно только после dispose().
     */
    dispose()

    if (projectorDevice != null) {
        isUndecorated = true
        isAlwaysOnTop = true

        /*
         * Клавиатура остаётся в основном окне
         * HolyPresenter.
         */
        focusableWindowState = false
        isAutoRequestFocus = false

        bounds = projectorDevice
            .defaultConfiguration
            .bounds
    } else {
        /*
         * Режим предпросмотра при одном мониторе.
         */
        isUndecorated = false
        isAlwaysOnTop = false
        focusableWindowState = true
        isAutoRequestFocus = true

        setSize(
            PREVIEW_WIDTH,
            PREVIEW_HEIGHT
        )
        setLocationRelativeTo(null)
    }
    isVisible = true
    validate()
    toFront()
}