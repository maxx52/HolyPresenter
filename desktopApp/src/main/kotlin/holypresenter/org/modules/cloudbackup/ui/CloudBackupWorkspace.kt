package holypresenter.org.modules.cloudbackup.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import holypresenter.org.platform.backup.BackupOptions
import holypresenter.org.platform.cloud.yandex.YandexCloudBackupService
import holypresenter.org.platform.cloud.yandex.YandexRemoteBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CloudBackupWorkspace(
    cloudService: YandexCloudBackupService
) {
    val scope = rememberCoroutineScope()
    var clientId by remember { mutableStateOf(cloudService.clientId()) }
    var connected by remember { mutableStateOf(cloudService.isConnected()) }
    var waitingForCode by remember { mutableStateOf(false) }
    var confirmationCode by remember { mutableStateOf("") }
    var includeImages by remember { mutableStateOf(true) }
    var includeAudio by remember { mutableStateOf(true) }
    var includeVideo by remember { mutableStateOf(false) }
    var remoteBackups by remember { mutableStateOf(emptyList<YandexRemoteBackup>()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var backupToDelete by remember { mutableStateOf<YandexRemoteBackup?>(null) }

    fun execute(action: suspend () -> Unit) {
        if (busy) return
        scope.launch {
            busy = true
            error = null
            message = null
            runCatching { action() }
                .onFailure { throwable ->
                    error = throwable.message ?: "Неизвестная ошибка"
                }
            busy = false
        }
    }

    fun refresh() = execute {
        remoteBackups = withContext(Dispatchers.IO) {
            cloudService.listRemoteBackups()
        }
    }

    LaunchedEffect(connected) {
        if (connected) refresh()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            Text("Резервные копии", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Сохраните рабочее состояние на Яндекс Диске и восстановите его на другом компьютере.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().widthIn(max = 900.dp)) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Яндекс Диск", style = MaterialTheme.typography.titleLarge)

                    if (connected) {
                        Text(
                            "● Подключён",
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                enabled = !busy,
                                onClick = { refresh() }
                            ) { Text("Обновить список") }
                            TextButton(
                                enabled = !busy,
                                onClick = {
                                    cloudService.disconnect()
                                    connected = false
                                    remoteBackups = emptyList()
                                    message = "Яндекс Диск отключён от этого компьютера"
                                }
                            ) { Text("Отключить") }
                        }
                    } else {
                        OutlinedTextField(
                            value = clientId,
                            onValueChange = { clientId = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Client ID приложения Яндекс OAuth") },
                            supportingText = {
                                Text("После регистрации официального приложения это поле будет заполнено автоматически.")
                            }
                        )

                        if (!waitingForCode) {
                            Button(
                                enabled = !busy && clientId.isNotBlank(),
                                onClick = {
                                    cloudService.saveClientId(clientId)
                                    execute {
                                        withContext(Dispatchers.IO) {
                                            cloudService.beginAuthorization()
                                        }
                                        waitingForCode = true
                                        message = "Разрешите доступ в браузере и введите показанный код"
                                    }
                                }
                            ) { Text("Подключить Яндекс Диск") }
                        } else {
                            Text("Введите код, показанный Яндексом в браузере")
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = confirmationCode,
                                    onValueChange = { value ->
                                        confirmationCode = value
                                            .filter(Char::isLetterOrDigit)
                                            .take(64)
                                    },
                                    singleLine = true,
                                    label = { Text("Код подтверждения") }
                                )
                                Button(
                                    enabled = !busy && confirmationCode.isNotBlank(),
                                    onClick = {
                                        execute {
                                            withContext(Dispatchers.IO) {
                                                cloudService.finishAuthorization(confirmationCode)
                                            }
                                            connected = true
                                            waitingForCode = false
                                            confirmationCode = ""
                                            message = "Яндекс Диск успешно подключён"
                                        }
                                    }
                                ) { Text("Подтвердить") }
                            }

                            TextButton(
                                enabled = !busy,
                                onClick = {
                                    confirmationCode = ""
                                    execute {
                                        withContext(Dispatchers.IO) {
                                            cloudService.beginAuthorization()
                                        }
                                        message = "В браузере открыт новый код подтверждения"
                                    }
                                }
                            ) {
                                Text("Получить новый код")
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().widthIn(max = 900.dp)) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Что сохранять", style = MaterialTheme.typography.titleLarge)
                    BackupSwitch("Изображения", includeImages) { includeImages = it }
                    BackupSwitch("Аудиофайлы", includeAudio) { includeAudio = it }
                    BackupSwitch("Видео (может занять много места)", includeVideo) { includeVideo = it }

                    Button(
                        enabled = connected && !busy,
                        onClick = {
                            execute {
                                val backup = withContext(Dispatchers.IO) {
                                    cloudService.createAndUpload(
                                        BackupOptions(includeImages, includeAudio, includeVideo)
                                    )
                                }
                                message = "Копия ${backup.file.name} загружена на Яндекс Диск"
                                remoteBackups = withContext(Dispatchers.IO) {
                                    cloudService.listRemoteBackups()
                                }
                            }
                        }
                    ) { Text("Создать резервную копию") }
                }
            }
        }

        if (busy) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Text("Выполняется операция…")
                }
            }
        }

        message?.let { text ->
            item {
                Text(text, color = MaterialTheme.colorScheme.secondary)
            }
        }

        error?.let { text ->
            item {
                Text(text, color = MaterialTheme.colorScheme.error)
            }
        }

        item {
            Text("Копии на Яндекс Диске", style = MaterialTheme.typography.titleLarge)
        }

        if (!connected) {
            item { Text("Подключите Яндекс Диск, чтобы увидеть резервные копии.") }
        } else if (!busy && remoteBackups.isEmpty()) {
            item { Text("Резервных копий пока нет.") }
        } else {
            items(remoteBackups, key = { it.path }) { backup ->
                RemoteBackupCard(
                    backup = backup,
                    enabled = !busy,
                    onRestore = {
                        execute {
                            withContext(Dispatchers.IO) {
                                cloudService.downloadAndScheduleRestore(backup)
                            }
                            message = "Копия проверена. Закройте и снова откройте HolyPresenter для восстановления."
                        }
                    },
                    onDelete = { backupToDelete = backup }
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    backupToDelete?.let { backup ->
        AlertDialog(
            onDismissRequest = { backupToDelete = null },
            title = { Text("Удалить резервную копию?") },
            text = { Text(backup.name) },
            confirmButton = {
                Button(onClick = {
                    backupToDelete = null
                    execute {
                        withContext(Dispatchers.IO) { cloudService.deleteRemoteBackup(backup) }
                        remoteBackups = withContext(Dispatchers.IO) { cloudService.listRemoteBackups() }
                        message = "Резервная копия удалена"
                    }
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { backupToDelete = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun BackupSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun RemoteBackupCard(
    backup: YandexRemoteBackup,
    enabled: Boolean,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().widthIn(max = 900.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(backup.name, fontWeight = FontWeight.SemiBold)
            Text(
                "${formatFileSize(backup.size)} · ${formatRemoteDate(backup.modified)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(enabled = enabled, onClick = onRestore) { Text("Восстановить") }
                TextButton(enabled = enabled, onClick = onDelete) { Text("Удалить") }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1_024 -> "$bytes Б"
    bytes < 1_048_576 -> "%.1f КБ".format(bytes / 1_024.0)
    bytes < 1_073_741_824 -> "%.1f МБ".format(bytes / 1_048_576.0)
    else -> "%.1f ГБ".format(bytes / 1_073_741_824.0)
}

private fun formatRemoteDate(value: String?): String {
    if (value.isNullOrBlank()) return "дата неизвестна"
    return runCatching {
        REMOTE_DATE_FORMAT.format(Instant.parse(value))
    }.getOrDefault(value)
}

private val REMOTE_DATE_FORMAT = DateTimeFormatter
    .ofPattern("dd.MM.yyyy HH:mm")
    .withZone(ZoneId.systemDefault())
