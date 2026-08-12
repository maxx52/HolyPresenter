package holypresenter.org.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Общая палитра HolyPresenter.
 *
 * Она опирается на живые цвета разделов Songs: голубой, зелёный, янтарный,
 * синий и красный. Модули, использующие MaterialTheme, получают её
 * автоматически без собственных настроек.
 */
private val HolyColorScheme = lightColorScheme(
    primary = Color(0xFF0EA5E9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0C4A6E),
    secondary = Color(0xFF22C55E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCFCE7),
    onSecondaryContainer = Color(0xFF14532D),
    tertiary = Color(0xFFF59E0B),
    onTertiary = Color(0xFF3B2500),
    tertiaryContainer = Color(0xFFFFEBC1),
    onTertiaryContainer = Color(0xFF5F4100),
    error = Color(0xFFEF4444),
    onError = Color.White,
    errorContainer = Color(0xFFFFE1E1),
    onErrorContainer = Color(0xFF7F1D1D),
    background = Color(0xFFFFFCF7),
    onBackground = Color(0xFF172033),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF172033),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFD8E0EA),
    scrim = Color(0xFF101827)
)

@Composable
fun HolyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HolyColorScheme,
        content = content
    )
}
