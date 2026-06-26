package holypresenter.org.modules.projector.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import holypresenter.org.modules.projector.model.ProjectorContent

@Composable
fun ProjectorWorkspace(
    currentContent: ProjectorContent
) {
    val text = when (currentContent) {
        ProjectorContent.Empty -> "Проектор пуст"
        is ProjectorContent.Text -> currentContent.value
        ProjectorContent.BlackScreen -> "Чёрный экран"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Модуль проектора",
            style = MaterialTheme.typography.headlineSmall
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text)
            }
        }
    }
}