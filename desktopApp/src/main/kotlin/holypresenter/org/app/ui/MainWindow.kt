package holypresenter.org.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import holypresenter.org.common.dock.DockManager
import holypresenter.org.common.dock.DockPosition
import holypresenter.org.common.module.HolyModule

@Composable
fun MainWindow(
    modules: List<HolyModule>
) {
    var selectedModule by remember {
        mutableStateOf(modules.firstOrNull())
    }

    val dockManager = remember {
        DockManager()
    }

    val panels = remember(modules) {
        dockManager.collectPanels(modules)
    }

    val leftPanels = panels.filter {
        it.position == DockPosition.LEFT
    }

    val rightPanels = panels.filter {
        it.position == DockPosition.RIGHT
    }

    val bottomPanels = panels.filter {
        it.position == DockPosition.BOTTOM
    }

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .padding(8.dp)
        ) {
            rightPanels.forEach { panel ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Column(
                        Modifier.padding(12.dp)
                    ) {
                        Text(
                            panel.title,
                            style = MaterialTheme.typography.titleSmall
                        )

                        Spacer(Modifier.height(8.dp))

                        panel.content()
                    }
                }
            }
        }

        VerticalDivider()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            selectedModule?.Workspace()
        }
    }
}