package holypresenter.org.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import holypresenter.org.platform.api.docking.DockPanelState
import holypresenter.org.platform.api.docking.DockPosition
import holypresenter.org.platform.api.module.HolyModule
import holypresenter.org.platform.core.DockManager

@Composable
fun MainWindow(
    modules: List<HolyModule>
) {
    var selectedModule by remember {
        mutableStateOf(modules.firstOrNull())
    }

    var modulesSidebarExpanded by remember {
        mutableStateOf(true)
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
            expanded = modulesSidebarExpanded,
            onToggleExpanded = {
                modulesSidebarExpanded = !modulesSidebarExpanded
            },
            hiddenPanels = hiddenPanels,
            onModuleClick = {
                selectedModule = it
            },
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

        if (rightPanels.isNotEmpty()) {
            VerticalDivider()

            DockSidePanel(
                panels = rightPanels,
                onHidePanel = dockManager::hide
            )
        }
    }
}

@Composable
private fun ModuleListPanel(
    modules: List<HolyModule>,
    selectedModule: HolyModule?,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    hiddenPanels: List<DockPanelState>,
    onModuleClick: (HolyModule) -> Unit,
    onShowPanel: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .width(if (expanded) 240.dp else 72.dp)
            .fillMaxHeight()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (expanded) {
                Text(
                    text = "Модули",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            TextButton(
                onClick = onToggleExpanded
            ) {
                Text(if (expanded) "◀" else "▶")
            }
        }

        Spacer(Modifier.height(12.dp))

        modules.forEach { module ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onModuleClick(module)
                    },
                color = if (module == selectedModule)
                    MaterialTheme.colorScheme.secondaryContainer
                else
                    MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Пока вместо иконки первая буква
                    Text(
                        moduleIcon(module.metadata.id),
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (expanded) {
                        Text(
                            module.metadata.name
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (expanded && hiddenPanels.isNotEmpty()) {
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

private fun moduleIcon(id: String): String =
    when (id) {
        "songs" -> "🎵"
        "projector" -> "📺"
        "welcome" -> "👋"
        "presentation-test" -> "🧪"
        else -> "📦"
    }