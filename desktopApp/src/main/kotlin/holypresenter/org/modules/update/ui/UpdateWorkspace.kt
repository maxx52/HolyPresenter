package holypresenter.org.modules.update.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import holypresenter.org.app.AppVersion
import holypresenter.org.platform.update.ApplicationUpdate
import holypresenter.org.platform.update.ApplicationUpdateService
import holypresenter.org.platform.update.UpdateCheckResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File
import java.net.URI

private sealed interface UpdateUiState {
    data object Checking : UpdateUiState
    data class Current(val version: String) : UpdateUiState
    data class Available(val update: ApplicationUpdate) : UpdateUiState
    data class Downloading(val update: ApplicationUpdate) : UpdateUiState
    data class Ready(val update: ApplicationUpdate, val installer: File) : UpdateUiState
    data class Installing(val update: ApplicationUpdate) : UpdateUiState
    data class Failed(val message: String, val update: ApplicationUpdate? = null) : UpdateUiState
}

@Composable
fun UpdateWorkspace(updateService: ApplicationUpdateService) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<UpdateUiState>(UpdateUiState.Checking) }

    fun check() {
        state = UpdateUiState.Checking
        scope.launch {
            state = runCatching {
                withContext(Dispatchers.IO) { updateService.checkForUpdates() }
            }.fold(
                onSuccess = { result ->
                    when (result) {
                        is UpdateCheckResult.Available -> UpdateUiState.Available(result.update)
                        is UpdateCheckResult.UpToDate -> UpdateUiState.Current(result.currentVersion)
                    }
                },
                onFailure = { error ->
                    UpdateUiState.Failed(error.message ?: "Не удалось проверить обновления.")
                }
            )
        }
    }

    LaunchedEffect(Unit) { check() }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 920.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Обновления HolyPresenter",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Установленная версия: ${AppVersion.VERSION}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when (val current = state) {
                UpdateUiState.Checking -> LoadingCard("Проверяем официальный выпуск на GitHub…")
                is UpdateUiState.Downloading -> LoadingCard("Скачиваем и проверяем HolyPresenter ${current.update.version}…")
                is UpdateUiState.Installing -> LoadingCard("Закрываем программу и запускаем обновление…")
                is UpdateUiState.Current -> CurrentVersionCard(current.version, ::check)
                is UpdateUiState.Available -> AvailableUpdateCard(
                    update = current.update,
                    onDownload = {
                        state = UpdateUiState.Downloading(current.update)
                        scope.launch {
                            state = runCatching {
                                withContext(Dispatchers.IO) {
                                    updateService.download(current.update)
                                }
                            }.fold(
                                onSuccess = { installer -> UpdateUiState.Ready(current.update, installer) },
                                onFailure = { error ->
                                    UpdateUiState.Failed(
                                        error.message ?: "Не удалось скачать обновление.",
                                        current.update
                                    )
                                }
                            )
                        }
                    },
                    onOpenRelease = { openBrowser(current.update.releasePageUrl) }
                )

                is UpdateUiState.Ready -> ReadyToInstallCard(
                    update = current.update,
                    onInstall = {
                        state = UpdateUiState.Installing(current.update)
                        updateService.installAfterExit(current.update, current.installer)
                            .onFailure { error ->
                                state = UpdateUiState.Failed(
                                    error.message ?: "Не удалось запустить обновление.",
                                    current.update
                                )
                            }
                    }
                )

                is UpdateUiState.Failed -> ErrorCard(
                    message = current.message,
                    onRetry = {
                        val update = current.update
                        if (update == null) {
                            check()
                        } else {
                            state = UpdateUiState.Available(update)
                        }
                    }
                )
            }

            Text(
                text = "Настройки, планы, песни и установленные модули сохраняются. " +
                        "Перед установкой Windows может запросить разрешение администратора.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LoadingCard(message: String) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(message, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun CurrentVersionCard(version: String, onCheck: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("У вас последняя версия", style = MaterialTheme.typography.titleLarge)
            Text("HolyPresenter $version готов к работе.")
            OutlinedButton(onClick = onCheck) { Text("Проверить ещё раз") }
        }
    }
}

@Composable
private fun AvailableUpdateCard(
    update: ApplicationUpdate,
    onDownload: () -> Unit,
    onOpenRelease: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Доступна версия ${update.version}", style = MaterialTheme.typography.titleLarge)
            Text(update.title, fontWeight = FontWeight.SemiBold)
            if (update.notes.isNotBlank()) {
                HorizontalDivider()
                Text(update.notes, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onDownload) { Text("Скачать обновление") }
                OutlinedButton(onClick = onOpenRelease) { Text("Что изменилось") }
            }
        }
    }
}

@Composable
private fun ReadyToInstallCard(update: ApplicationUpdate, onInstall: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Обновление проверено и готово", style = MaterialTheme.typography.titleLarge)
            Text(
                "HolyPresenter закроется, Windows установит версию ${update.version}, " +
                        "после чего программа откроется снова."
            )
            Button(onClick = onInstall) { Text("Обновить и перезапустить") }
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Обновление не выполнено",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )
            Text(message)
            OutlinedButton(onClick = onRetry) { Text("Повторить") }
        }
    }
}

private fun openBrowser(url: String) {
    runCatching {
        if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI.create(url))
    }
}
