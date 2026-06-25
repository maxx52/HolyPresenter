package holypresenter.org.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import holypresenter.org.common.dock.DockManager
import holypresenter.org.common.dock.DockPanelState
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

    LaunchedEffect(modules) {
        dockManager.registerModules(modules)
    }

    val rightPanels = dockManager.panels.filter {
        it.visible && it.panel.position == DockPosition.RIGHT
    }

    val hiddenPanels = dockManager.panels.filter {
        !it.visible
    }

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        ModuleListPanel(
            modules = modules,
            selectedModule = selectedModule,
            onModuleClick = {
                selectedModule = it
            },
            hiddenPanels = hiddenPanels,
            onShowPanel = dockManager::show
        )

        VerticalDivider()

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            selectedModule?.Workspace()
        }

        VerticalDivider()

        DockSidePanel(
            panels = rightPanels,
            onHidePanel = dockManager::hide
        )
    }
}

@Composable
private fun ModuleListPanel(
    modules: List<HolyModule>,
    selectedModule: HolyModule?,
    hiddenPanels: List<DockPanelState>,
    onModuleClick: (HolyModule) -> Unit,
    onShowPanel: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .width(240.dp)
            .fillMaxHeight()
            .padding(12.dp)
    ) {
        Text(
            text = "Модули",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(12.dp))

        modules.forEach { module ->
            Text(
                text = module.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onModuleClick(module)
                    }
                    .padding(12.dp),
                color = if (module == selectedModule)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }

        if (hiddenPanels.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))

            Text(
                text = "Скрытые панели",
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(Modifier.height(8.dp))

            hiddenPanels.forEach { panelState ->
                TextButton(
                    onClick = {
                        onShowPanel(panelState.panel.id)
                    }
                ) {
                    Text(panelState.panel.title)
                }
            }
        }
    }
}

@Composable
private fun DockSidePanel(
    panels: List<DockPanelState>,
    onHidePanel: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .padding(8.dp)
    ) {
        panels.forEach { panelState ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = panelState.panel.title,
                        style = MaterialTheme.typography.titleSmall
                    )

                    Spacer(Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            onHidePanel(panelState.panel.id)
                        }
                    ) {
                        Text("Скрыть")
                    }

                    panelState.panel.content.Content()
                }
            }
        }
    }
}