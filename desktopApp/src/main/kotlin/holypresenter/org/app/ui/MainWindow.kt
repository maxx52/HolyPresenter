package holypresenter.org.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import holypresenter.org.platform.api.docking.DockPanelState
import holypresenter.org.platform.api.docking.DockPosition
import holypresenter.org.platform.api.module.HolyModule
import holypresenter.org.platform.core.DockManager
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun MainWindow(
    modules: List<HolyModule>,
    onDisableModule: (String) -> Unit,
    onDeleteModule: (String) -> Boolean,
    canDeleteModule: (String) -> Boolean,
    disabledBuiltinModuleIds: Set<String>,
    onEnableBuiltinModule: (String) -> Boolean,
    disabledExternalModules: List<HolyModule>,
    onEnableExternalModule: (String) -> Boolean,
    onImportModule: (File) -> String
) {
    var selectedModule by remember {
        mutableStateOf(modules.firstOrNull())
    }

    var modulesSidebarExpanded by remember {
        mutableStateOf(true)
    }

    var rightPanelOverlayOpen by remember {
        mutableStateOf(false)
    }

    val dockManager = remember {
        DockManager()
    }

    LaunchedEffect(modules) {
        dockManager.registerModules(modules)
        if (selectedModule !in modules) {
            selectedModule = modules.firstOrNull()
        }
    }

    val rightPanels = dockManager.panels.filter {
        it.visible && it.panel.position == DockPosition.RIGHT
    }

    val hiddenPanels = dockManager.panels.filter {
        !it.visible
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val compactModulesSidebar = maxWidth < 1280.dp
        val useRightPanelOverlay = maxWidth < 1050.dp && rightPanels.isNotEmpty()

        LaunchedEffect(compactModulesSidebar) {
            if (compactModulesSidebar) {
                modulesSidebarExpanded = false
            }
        }

        LaunchedEffect(useRightPanelOverlay) {
            if (!useRightPanelOverlay) {
                rightPanelOverlayOpen = false
            }
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                ModuleListPanel(
                    modules = modules,
                    selectedModule = selectedModule,
                    expanded = modulesSidebarExpanded,
                    onToggleExpanded = {
                        modulesSidebarExpanded =
                            !modulesSidebarExpanded
                    },
                    hiddenPanels = hiddenPanels,
                    onModuleClick = {
                        selectedModule = it
                    },
                    onDisableModule = onDisableModule,
                    onDeleteModule = onDeleteModule,
                    canDeleteModule = canDeleteModule,
                    disabledBuiltinModuleIds = disabledBuiltinModuleIds,
                    onEnableBuiltinModule = onEnableBuiltinModule,
                    disabledExternalModules = disabledExternalModules,
                    onEnableExternalModule = onEnableExternalModule,
                    onImportModule = onImportModule,
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

                if (
                    !useRightPanelOverlay &&
                    rightPanels.isNotEmpty()
                ) {
                    VerticalDivider()

                    DockSidePanel(
                        panels = rightPanels,
                        onHidePanel = dockManager::hide
                    )
                }
            }

            if (useRightPanelOverlay) {
                FilledTonalButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    onClick = {
                        rightPanelOverlayOpen = true
                    }
                ) {
                    Text(
                        text = rightPanels
                            .first()
                            .panel
                            .title
                    )
                }

                if (rightPanelOverlayOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme
                                    .scrim
                                    .copy(alpha = 0.32f)
                            )
                            .clickable {
                                rightPanelOverlayOpen = false
                            }
                    )

                    Surface(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(304.dp)
                            .fillMaxHeight()
                            .padding(8.dp),
                        shape =
                            MaterialTheme.shapes.large,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = 16.dp,
                                        end = 8.dp,
                                        top = 8.dp
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = rightPanels
                                        .first()
                                        .panel
                                        .title,
                                    style = MaterialTheme
                                        .typography
                                        .titleMedium,
                                    modifier = Modifier.weight(1f)
                                )

                                TextButton(
                                    onClick = {
                                        rightPanelOverlayOpen = false
                                    }
                                ) {
                                    Text("Закрыть")
                                }
                            }

                            DockSidePanel(
                                panels = rightPanels,
                                onHidePanel = dockManager::hide
                            )
                        }
                    }
                }
            }
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
    onDisableModule: (String) -> Unit,
    onDeleteModule: (String) -> Boolean,
    canDeleteModule: (String) -> Boolean,
    disabledBuiltinModuleIds: Set<String>,
    onEnableBuiltinModule: (String) -> Boolean,
    disabledExternalModules: List<HolyModule>,
    onEnableExternalModule: (String) -> Boolean,
    onImportModule: (File) -> String,
    onShowPanel: (String) -> Unit
) {
    var importResult by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier
            .width(if (expanded) 240.dp else 72.dp)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
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

        if (expanded) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    chooseModuleJar()?.let { importResult = onImportModule(it) }
                }
            ) { Text("+ Импортировать модуль") }
            Spacer(Modifier.height(12.dp))
        }

        modules.forEach { module ->
            var menuOpen by remember(module.metadata.id) { mutableStateOf(false) }
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
                    Text(
                        text = module.metadata.icon,
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (expanded) {
                        Text(
                            module.metadata.name,
                            modifier = Modifier.weight(1f)
                        )
                        Box {
                            TextButton(onClick = { menuOpen = true }) { Text("⋮") }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Отключить") },
                                    onClick = { menuOpen = false; onDisableModule(module.metadata.id) }
                                )
                                if (canDeleteModule(module.metadata.id)) {
                                    DropdownMenuItem(
                                        text = { Text("Удалить") },
                                        onClick = { menuOpen = false; onDeleteModule(module.metadata.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (expanded && disabledBuiltinModuleIds.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Отключённые встроенные", style = MaterialTheme.typography.titleSmall)
            disabledBuiltinModuleIds.forEach { id ->
                TextButton(onClick = { onEnableBuiltinModule(id) }) { Text("Включить: $id") }
            }
        }

        if (expanded && disabledExternalModules.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Отключённые модули",
                style = MaterialTheme.typography.titleSmall
            )
            disabledExternalModules.forEach { module ->
                TextButton(
                    onClick = {
                        onEnableExternalModule(module.metadata.id)
                    }
                ) {
                    Text("Включить: ${module.metadata.name}")
                }
            }
        }

        importResult?.let { message ->
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodySmall)
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

private fun chooseModuleJar(): File? = JFileChooser().run {
    dialogTitle = "Импорт модуля HolyPresenter"
    isAcceptAllFileFilterUsed = false
    fileFilter = FileNameExtensionFilter("Модуль HolyPresenter (*.jar)", "jar")
    if (showOpenDialog(null) == JFileChooser.APPROVE_OPTION) selectedFile else null
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
