package holypresenter.org.common.dock

import androidx.compose.runtime.Composable

data class DockPanel(
    val id: String,
    val title: String,
    val position: DockPosition,
    val content: @Composable () -> Unit
)