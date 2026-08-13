@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package holypresenter.org.modules.aiassistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import holypresenter.org.modules.aiassistant.AiAssistantPlanState
import holypresenter.org.modules.aiassistant.AiAssistantPlanStateCodec
import holypresenter.org.platform.ai.AiAssistantSettings
import holypresenter.org.platform.ai.AiAssistantStorage
import holypresenter.org.platform.ai.ComfyUiProvider
import holypresenter.org.platform.ai.ComfyUiStatus
import holypresenter.org.platform.ai.OllamaProvider
import holypresenter.org.platform.ai.OllamaStatus
import holypresenter.org.platform.ai.OpenAiApiKeyStore
import holypresenter.org.platform.api.ai.AiCostEstimate
import holypresenter.org.platform.api.ai.AiGenerationKind
import holypresenter.org.platform.api.ai.AiGenerationRequest
import holypresenter.org.platform.api.ai.AiGenerationResult
import holypresenter.org.platform.api.ai.AiImageQuality
import holypresenter.org.platform.api.ai.AiProvider
import holypresenter.org.platform.api.ai.AiProviderRegistry
import holypresenter.org.platform.api.module.ModuleContext
import holypresenter.org.platform.api.planner.PlannerItem
import holypresenter.org.platform.api.planner.PlannerReference
import holypresenter.org.platform.api.planner.PlannerService
import holypresenter.org.platform.api.presentation.theme.PresentationBackgroundType
import holypresenter.org.platform.api.projection.ProjectionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI
import java.util.Locale

private const val OPENAI_API_KEYS_URL = "https://platform.openai.com/api-keys"
private const val OPENAI_BILLING_URL =
    "https://platform.openai.com/settings/organization/billing/overview"

@Composable
fun AiAssistantWorkspace(
    context: ModuleContext,
    storage: AiAssistantStorage,
    apiKeyStore: OpenAiApiKeyStore,
    ollamaProvider: OllamaProvider,
    comfyUiProvider: ComfyUiProvider
) {
    val scope = rememberCoroutineScope()
    val registry = remember(context) { context.services.get(AiProviderRegistry::class) }
    val projection = remember(context) { context.services.get(ProjectionService::class) }
    val planner = remember(context) { context.services.get(PlannerService::class) }

    val initialKeyConfigured = remember(apiKeyStore) { apiKeyStore.isConfigured() }
    val initialSettings = remember(storage, initialKeyConfigured) {
        storage.loadSettings().let { loaded ->
            if (loaded.providerId == "openai" && !initialKeyConfigured) {
                loaded.copy(providerId = "free-templates")
            } else {
                loaded
            }
        }
    }
    var settings by remember { mutableStateOf(initialSettings) }
    var kind by remember { mutableStateOf(AiGenerationKind.TEXT) }
    var prompt by remember { mutableStateOf("") }
    var apiKeyDraft by remember { mutableStateOf("") }
    var keyConfigured by remember { mutableStateOf(initialKeyConfigured) }
    var spentThisMonth by remember { mutableStateOf(storage.spentThisMonth()) }
    var monthlyLimitDraft by remember { mutableStateOf(formatEditable(settings.monthlyLimitUsd)) }
    var showAdvanced by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var progressMessage by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<AiGenerationResult?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingRequest by remember { mutableStateOf<Pair<AiGenerationRequest, AiCostEstimate>?>(null) }
    var ollamaStatus by remember { mutableStateOf<OllamaStatus>(OllamaStatus.Checking) }
    var ollamaBusy by remember { mutableStateOf(false) }
    var ollamaProgress by remember { mutableStateOf(0) }
    var ollamaProgressMessage by remember { mutableStateOf("") }
    var comfyUiStatus by remember { mutableStateOf<ComfyUiStatus>(ComfyUiStatus.Checking) }
    var comfyUiBusy by remember { mutableStateOf(false) }

    val providers = registry?.providers(kind).orEmpty()
    val provider = providers.firstOrNull { it.id == settings.providerId }
        ?: providers.firstOrNull()
    val currentRequest = remember(kind, prompt, settings) {
        settings.request(kind, prompt.trim())
    }
    val estimate = remember(provider, currentRequest) {
        provider?.let { selected -> runCatching { selected.estimate(currentRequest) } }
    }
    val providerReady = when (provider?.id) {
        "openai" -> keyConfigured
        ollamaProvider.id -> ollamaStatus == OllamaStatus.Ready
        comfyUiProvider.id -> comfyUiStatus is ComfyUiStatus.Ready
        null -> false
        else -> true
    }

    LaunchedEffect(Unit) {
        if (initialSettings.providerId == "free-templates") {
            storage.saveSettings(initialSettings)
        }
        ollamaStatus = ollamaProvider.status()
        comfyUiStatus = comfyUiProvider.status()
    }

    fun saveSettings(updated: AiAssistantSettings) {
        settings = updated
        storage.saveSettings(updated)
    }

    fun startGeneration(request: AiGenerationRequest, selectedProvider: AiProvider) {
        val calculated = selectedProvider.estimate(request)
        if (!storage.canSpend(calculated.usd, settings.monthlyLimitUsd)) {
            error = "Запрос превысит месячный лимит. Увеличьте лимит осознанно в настройках."
            return
        }
        scope.launch {
            busy = true
            progress = 0
            progressMessage = "Начинаем…"
            error = null
            message = null
            result = null
            runCatching {
                selectedProvider.generate(request) { percent, text ->
                    scope.launch {
                        progress = percent
                        progressMessage = text
                    }
                }
            }.onSuccess { generated ->
                result = generated
                spentThisMonth = withContext(Dispatchers.IO) {
                    storage.recordCost(generated.costUsd)
                }
                message = "Готово. Учтено ${formatUsd(generated.costUsd)}"
            }.onFailure { throwable ->
                error = throwable.message ?: "Не удалось выполнить запрос"
            }
            busy = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            Text("ИИ-помощник", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Подготовьте текст и изображения бесплатно или подключите облачную генерацию. " +
                    "Все расходы видны до запуска.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth().widthIn(max = 1_100.dp)) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Бесплатный режим", style = MaterialTheme.typography.titleLarge)
                    Text("✓ Готовые офлайн-шаблоны работают сразу на любом компьютере")
                    Text("✓ Qwen3 создаёт текст локально без оплаты и API-ключа")
                    Text("✓ Бесплатные цветные фоны создаются даже на слабом ПК")
                    Text("✓ ComfyUI создаёт нейросетевые изображения локально")
                    Text(
                        "Для локальной нейрогенерации изображений рекомендуется отдельная видеокарта. " +
                            "Видео пока создаётся через облачный провайдер.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            ApiKeyCard(
                apiKeyDraft = apiKeyDraft,
                configured = keyConfigured,
                environmentKey = apiKeyStore.usesEnvironmentKey(),
                enabled = !busy,
                onDraftChange = { apiKeyDraft = it },
                onSave = {
                    runCatching { apiKeyStore.save(apiKeyDraft) }
                        .onSuccess {
                            apiKeyDraft = ""
                            keyConfigured = true
                            message = "API-ключ сохранён и защищён Windows"
                            error = null
                        }
                        .onFailure { throwable ->
                            error = throwable.message ?: "Не удалось сохранить API-ключ"
                        }
                },
                onClear = {
                    apiKeyStore.clear()
                    keyConfigured = apiKeyStore.isConfigured()
                    message = "Сохранённый API-ключ удалён"
                },
                onOpenApiKeys = {
                    runCatching { openInBrowser(OPENAI_API_KEYS_URL) }
                        .onFailure { throwable ->
                            error = throwable.message ?: "Не удалось открыть страницу API-ключей"
                        }
                },
                onOpenBilling = {
                    runCatching { openInBrowser(OPENAI_BILLING_URL) }
                        .onFailure { throwable ->
                            error = throwable.message ?: "Не удалось открыть страницу оплаты API"
                        }
                }
            )
        }

        item {
            BudgetCard(
                spentUsd = spentThisMonth,
                limitUsd = settings.monthlyLimitUsd,
                limitDraft = monthlyLimitDraft,
                enabled = !busy,
                onLimitDraftChange = { monthlyLimitDraft = it.filterBudgetCharacters() },
                onSave = {
                    val value = monthlyLimitDraft.replace(',', '.').toDoubleOrNull()
                    if (value == null || value <= 0.0 || value > 10_000.0) {
                        error = "Укажите месячный лимит от 0.01 до 10000 долларов"
                    } else {
                        saveSettings(settings.copy(monthlyLimitUsd = value))
                        monthlyLimitDraft = formatEditable(value)
                        message = "Месячный лимит сохранён"
                        error = null
                    }
                }
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().widthIn(max = 1_100.dp)) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Что создать", style = MaterialTheme.typography.titleLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GenerationKindChip(AiGenerationKind.TEXT, kind) { kind = it }
                        GenerationKindChip(AiGenerationKind.IMAGE, kind) { kind = it }
                        GenerationKindChip(AiGenerationKind.VIDEO, kind) { kind = it }
                    }

                    if (providers.size > 1) {
                        Text("Провайдер", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            providers.forEach { available ->
                                FilterChip(
                                    selected = provider?.id == available.id,
                                    onClick = { saveSettings(settings.copy(providerId = available.id)) },
                                    label = { Text(available.displayName) }
                                )
                            }
                        }
                    }

                    if (provider?.id == ollamaProvider.id) {
                        OllamaSetupCard(
                            status = ollamaStatus,
                            model = ollamaProvider.model,
                            busy = ollamaBusy,
                            progress = ollamaProgress,
                            progressMessage = ollamaProgressMessage,
                            onCheck = {
                                scope.launch {
                                    ollamaBusy = true
                                    ollamaStatus = OllamaStatus.Checking
                                    ollamaStatus = ollamaProvider.status()
                                    ollamaBusy = false
                                }
                            },
                            onInstallOllama = {
                                runCatching { openInBrowser(OllamaProvider.DOWNLOAD_URL) }
                                    .onFailure { throwable ->
                                        error = throwable.message
                                            ?: "Не удалось открыть страницу загрузки Ollama"
                                    }
                            },
                            onInstallModel = {
                                scope.launch {
                                    ollamaBusy = true
                                    ollamaProgress = 0
                                    ollamaProgressMessage = "Подключаемся к Ollama…"
                                    ollamaStatus = ollamaProvider.installModel { percent, text ->
                                        scope.launch {
                                            ollamaProgress = percent
                                            ollamaProgressMessage = text
                                        }
                                    }
                                    ollamaBusy = false
                                    if (ollamaStatus == OllamaStatus.Ready) {
                                        message = "Бесплатная модель ${ollamaProvider.model} установлена"
                                        error = null
                                    }
                                }
                            }
                        )
                    }

                    if (provider?.id == comfyUiProvider.id) {
                        ComfyUiSetupCard(
                            status = comfyUiStatus,
                            busy = comfyUiBusy,
                            onCheck = {
                                scope.launch {
                                    comfyUiBusy = true
                                    comfyUiStatus = ComfyUiStatus.Checking
                                    comfyUiStatus = comfyUiProvider.status()
                                    comfyUiBusy = false
                                }
                            },
                            onInstallComfyUi = {
                                runCatching { openInBrowser(ComfyUiProvider.DOWNLOAD_URL) }
                                    .onFailure { throwable ->
                                        error = throwable.message
                                            ?: "Не удалось открыть страницу загрузки ComfyUI"
                                    }
                            },
                            onInstallModel = {
                                runCatching { openInBrowser(ComfyUiProvider.INSTALL_GUIDE_URL) }
                                    .onFailure { throwable ->
                                        error = throwable.message
                                            ?: "Не удалось открыть инструкцию ComfyUI"
                                    }
                            }
                        )
                    }

                    PromptPresets(kind) { preset -> prompt = preset }

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it.take(32_000) },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        label = { Text("Запрос") },
                        placeholder = { Text(promptHint(kind)) },
                        enabled = !busy
                    )

                    val estimateValue = estimate?.getOrNull()
                    when {
                        provider == null -> Text(
                            "Нет провайдера для выбранного типа генерации",
                            color = MaterialTheme.colorScheme.error
                        )

                        estimateValue != null -> Text(
                            "Предварительная стоимость: ${formatUsd(estimateValue.usd)} · ${estimateValue.description}"
                        )

                        estimate?.exceptionOrNull() != null -> Text(
                            estimate.exceptionOrNull()?.message ?: "Невозможно рассчитать стоимость",
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (kind == AiGenerationKind.VIDEO) {
                        Text(
                            "Видео OpenAI Sora 2 доступно временно: API заявлен к отключению 24.09.2026. " +
                                    "HolyPresenter использует сменный интерфейс провайдеров.",
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        enabled = !busy && providerReady && prompt.isNotBlank() &&
                                provider != null && estimate?.isSuccess == true,
                        onClick = {
                            val selected = provider ?: return@Button
                            val calculated = selected.estimate(currentRequest)
                            if (!storage.canSpend(calculated.usd, settings.monthlyLimitUsd)) {
                                error = "Запрос превысит месячный лимит ${formatUsd(settings.monthlyLimitUsd)}"
                            } else {
                                pendingRequest = currentRequest to calculated
                            }
                        }
                    ) {
                        Text(if (busy) "Создаём…" else "Создать")
                    }

                    if (provider?.id == "openai" && !keyConfigured) {
                        Text(
                            "Для генерации добавьте собственный API-ключ OpenAI.",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (provider?.id == ollamaProvider.id && !providerReady) {
                        Text(
                            "Запустите Ollama и установите бесплатную модель перед генерацией.",
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    } else if (provider?.id == comfyUiProvider.id && !providerReady) {
                        Text(
                            "Запустите ComfyUI и установите checkpoint-модель перед генерацией.",
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }

        item {
            AdvancedSettingsCard(
                expanded = showAdvanced,
                settings = settings,
                kind = kind,
                providerId = provider?.id,
                enabled = !busy,
                onExpandedChange = { showAdvanced = it },
                onSettingsChange =(::saveSettings)
            )
        }

        if (busy) {
            item {
                OutlinedCard(modifier = Modifier.fillMaxWidth().widthIn(max = 1_100.dp)) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Column {
                            Text(progressMessage, fontWeight = FontWeight.SemiBold)
                            Text("$progress%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        message?.let { text ->
            item { Text(text, color = MaterialTheme.colorScheme.secondary) }
        }

        error?.let { text ->
            item { Text(text, color = MaterialTheme.colorScheme.error) }
        }

        result?.let { generated ->
            item {
                GenerationResultCard(
                    result = generated,
                    projectionAvailable = projection != null,
                    plannerAvailable = planner != null,
                    onShow = { projection?.show(generated.toPlanState().toProjectionContent()) },
                    onAddToPlanner = {
                        val state = generated.toPlanState()
                        planner?.add(
                            PlannerItem.Generic(
                                reference = PlannerReference(
                                    moduleId = "ai-assistant",
                                    itemId = AiAssistantPlanStateCodec.encode(state)
                                ),
                                title = generated.plannerTitle()
                            )
                        )
                        message = "Результат добавлен в план служения"
                    }
                )
            }
        }

        item { Spacer(Modifier.height(28.dp)) }
    }

    pendingRequest?.let { (request, calculated) ->
        AlertDialog(
            onDismissRequest = { if (!busy) pendingRequest = null },
            title = { Text("Подтвердить генерацию") },
            text = {
                val free = calculated.usd == 0.0
                Text(if (free) {
                    "Запрос будет выполнен бесплатно провайдером «${provider?.displayName}». " +
                        "С вашего счёта ничего не спишется."
                } else {
                    "Будет использован ваш API-ключ. Предварительная стоимость: " +
                        "${formatUsd(calculated.usd)}.\n\n" +
                        "Потрачено в этом месяце: ${formatUsd(spentThisMonth)} из " +
                        formatUsd(settings.monthlyLimitUsd)
                })
            },
            confirmButton = {
                Button(
                    onClick = {
                        val selected = provider
                        pendingRequest = null
                        if (selected != null) startGeneration(request, selected)
                    }
                ) { Text("Подтвердить") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRequest = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun ApiKeyCard(
    apiKeyDraft: String,
    configured: Boolean,
    environmentKey: Boolean,
    enabled: Boolean,
    onDraftChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onOpenApiKeys: () -> Unit,
    onOpenBilling: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth().widthIn(max = 1_100.dp)) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("OpenAI — необязательно", style = MaterialTheme.typography.titleLarge)
            Text(
                when {
                    environmentKey -> "● Используется ключ из OPENAI_API_KEY"
                    configured -> "● Ключ сохранён и защищён Windows"
                    else -> "Ключ ещё не добавлен"
                },
                color = if (configured) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = apiKeyDraft,
                onValueChange = { onDraftChange(it.trim().take(300)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = enabled && !environmentKey,
                visualTransformation = PasswordVisualTransformation(),
                label = { Text("Новый ключ OpenAI (sk-…)") },
                supportingText = {
                    Text("Ключ не попадает в резервную копию и не показывается после сохранения.")
                }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    enabled = enabled && !environmentKey && apiKeyDraft.isNotBlank(),
                    onClick = onSave
                ) { Text("Сохранить ключ") }
                TextButton(
                    enabled = enabled && configured && !environmentKey,
                    onClick = onClear
                ) { Text("Удалить ключ") }
            }
            HorizontalDivider()
            Text(
                "Ключ создаётся в аккаунте OpenAI. Оплата API не входит в подписку ChatGPT " +
                    "Plus и настраивается отдельно.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(enabled = enabled, onClick = onOpenApiKeys) {
                    Text("Получить API-ключ")
                }
                OutlinedButton(enabled = enabled, onClick = onOpenBilling) {
                    Text("Оплатить API")
                }
            }
        }
    }
}

@Composable
private fun OllamaSetupCard(
    status: OllamaStatus,
    model: String,
    busy: Boolean,
    progress: Int,
    progressMessage: String,
    onCheck: () -> Unit,
    onInstallOllama: () -> Unit,
    onInstallModel: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Бесплатная нейросеть на компьютере", style = MaterialTheme.typography.titleMedium)
            Text(
                "Qwen3 1.7B работает через Ollama без API-ключа и не отправляет текст в облако. " +
                    "Модель занимает ${OllamaProvider.MODEL_SIZE_LABEL}.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val statusText = when (status) {
                OllamaStatus.Checking -> "Проверяем Ollama…"
                OllamaStatus.NotRunning -> "Ollama не установлена или не запущена"
                OllamaStatus.ModelMissing -> "Ollama запущена, но модель $model ещё не установлена"
                OllamaStatus.Ready -> "● Бесплатная модель $model готова к работе"
                is OllamaStatus.Failed -> status.message
            }
            Text(
                statusText,
                color = when (status) {
                    OllamaStatus.Ready -> MaterialTheme.colorScheme.secondary
                    OllamaStatus.NotRunning, is OllamaStatus.Failed -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.tertiary
                }
            )
            if (busy && progressMessage.isNotBlank()) {
                Text("$progress% · $progressMessage", style = MaterialTheme.typography.bodySmall)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (status == OllamaStatus.NotRunning) {
                    OutlinedButton(enabled = !busy, onClick = onInstallOllama) {
                        Text("Скачать Ollama")
                    }
                }
                if (status == OllamaStatus.ModelMissing) {
                    Button(enabled = !busy, onClick = onInstallModel) {
                        Text("Установить модель")
                    }
                }
                OutlinedButton(enabled = !busy, onClick = onCheck) {
                    Text("Проверить снова")
                }
            }
            Text(
                "На очень слабом ПК ответ может появляться медленно. Бесплатные шаблоны " +
                    "работают быстрее и не требуют установки.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ComfyUiSetupCard(
    status: ComfyUiStatus,
    busy: Boolean,
    onCheck: () -> Unit,
    onInstallComfyUi: () -> Unit,
    onInstallModel: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Бесплатная генерация изображений",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "ComfyUI создаёт изображения локально, без API-ключа и оплаты. " +
                    "HolyPresenter подключается только к 127.0.0.1:8188.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val statusText = when (status) {
                ComfyUiStatus.Checking -> "Проверяем ComfyUI…"
                ComfyUiStatus.NotRunning -> "ComfyUI не установлена или не запущена"
                ComfyUiStatus.ModelMissing -> "ComfyUI запущена, но checkpoint-модель не установлена"
                is ComfyUiStatus.Ready -> "● ComfyUI готова · ${status.checkpoint}"
                is ComfyUiStatus.Failed -> status.message
            }
            Text(
                statusText,
                color = when (status) {
                    is ComfyUiStatus.Ready -> MaterialTheme.colorScheme.secondary
                    ComfyUiStatus.NotRunning, is ComfyUiStatus.Failed ->
                        MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.tertiary
                }
            )
            if (busy) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Проверяем локальный движок…")
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (status == ComfyUiStatus.NotRunning) {
                    OutlinedButton(enabled = !busy, onClick = onInstallComfyUi) {
                        Text("Скачать ComfyUI")
                    }
                }
                if (status == ComfyUiStatus.ModelMissing) {
                    Button(enabled = !busy, onClick = onInstallModel) {
                        Text("Как установить модель")
                    }
                }
                OutlinedButton(enabled = !busy, onClick = onCheck) {
                    Text("Проверить снова")
                }
            }
            Text(
                "ComfyUI запускается отдельно и не занимает память, пока выключена. " +
                    "На ПК без подходящей видеокарты используйте «Бесплатные фоны».",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun openInBrowser(url: String) {
    check(Desktop.isDesktopSupported()) { "Открытие браузера не поддерживается системой" }
    val desktop = Desktop.getDesktop()
    check(desktop.isSupported(Desktop.Action.BROWSE)) {
        "Открытие ссылок не поддерживается системой"
    }
    desktop.browse(URI.create(url))
}

@Composable
private fun BudgetCard(
    spentUsd: Double,
    limitUsd: Double,
    limitDraft: String,
    enabled: Boolean,
    onLimitDraftChange: (String) -> Unit,
    onSave: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth().widthIn(max = 1_100.dp)) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Контроль расходов", style = MaterialTheme.typography.titleLarge)
            Text("В этом месяце: ${formatUsd(spentUsd)} из ${formatUsd(limitUsd)}")
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = limitDraft,
                    onValueChange = onLimitDraftChange,
                    singleLine = true,
                    enabled = enabled,
                    label = { Text("Лимит, USD/месяц") }
                )
                OutlinedButton(enabled = enabled, onClick = onSave) {
                    Text("Сохранить")
                }
            }
            Text(
                "Начальный лимит — \$10. Для регулярного видео его нужно увеличить вручную.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Это локальная защита HolyPresenter на данном компьютере, а не лимит счёта OpenAI.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AdvancedSettingsCard(
    expanded: Boolean,
    settings: AiAssistantSettings,
    kind: AiGenerationKind,
    providerId: String?,
    enabled: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSettingsChange: (AiAssistantSettings) -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth().widthIn(max = 1_100.dp)) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Расширенные возможности", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "По умолчанию используются экономные и безопасные параметры.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = expanded,
                    enabled = enabled,
                    onCheckedChange = onExpandedChange
                )
            }

            if (expanded) {
                HorizontalDivider()
                when (kind) {
                    AiGenerationKind.TEXT -> if (providerId == "openai") {
                        Text("Текстовая модель OpenAI", style = MaterialTheme.typography.titleMedium)
                        ModelRadio("gpt-5.6-luna", "Luna — минимальная стоимость", settings.textModel, enabled) {
                            onSettingsChange(settings.copy(textModel = it))
                        }
                        ModelRadio("gpt-5.6-terra", "Terra — более сильная", settings.textModel, enabled) {
                            onSettingsChange(settings.copy(textModel = it))
                        }
                        ModelRadio("gpt-5.6-sol", "Sol — максимальные возможности", settings.textModel, enabled) {
                            onSettingsChange(settings.copy(textModel = it))
                        }
                    } else {
                        Text(
                            "Для бесплатного текстового режима параметры подобраны автоматически.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AiGenerationKind.IMAGE -> if (providerId == "openai") {
                        Text("Качество изображения", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AiImageQuality.entries.forEach { quality ->
                                FilterChip(
                                    selected = settings.imageQuality == quality,
                                    enabled = enabled,
                                    onClick = { onSettingsChange(settings.copy(imageQuality = quality)) },
                                    label = { Text(quality.title()) }
                                )
                            }
                        }
                    } else {
                        Text(
                            "Для бесплатных изображений выбран безопасный размер, подходящий " +
                                "для предпросмотра и проектора.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AiGenerationKind.VIDEO -> {
                        Text("Длительность видео", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(4, 8, 12).forEach { seconds ->
                                FilterChip(
                                    selected = settings.videoSeconds == seconds,
                                    enabled = enabled,
                                    onClick = { onSettingsChange(settings.copy(videoSeconds = seconds)) },
                                    label = { Text("$seconds сек.") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelRadio(
    model: String,
    title: String,
    selected: String,
    enabled: Boolean,
    onSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected == model,
                enabled = enabled,
                onClick = { onSelected(model) }
            )
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected == model,
            enabled = enabled,
            onClick = null
        )
        Text(title)
    }
}

@Composable
private fun GenerationKindChip(
    value: AiGenerationKind,
    selected: AiGenerationKind,
    onSelected: (AiGenerationKind) -> Unit
) {
    FilterChip(
        selected = selected == value,
        onClick = { onSelected(value) },
        label = { Text(value.title()) }
    )
}

@Composable
private fun PromptPresets(
    kind: AiGenerationKind,
    onSelect: (String) -> Unit
) {
    Text("Быстрые шаблоны", style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        presets(kind).forEach { (title, prompt) ->
            OutlinedButton(onClick = { onSelect(prompt) }) { Text(title) }
        }
    }
}

@Composable
private fun GenerationResultCard(
    result: AiGenerationResult,
    projectionAvailable: Boolean,
    plannerAvailable: Boolean,
    onShow: () -> Unit,
    onAddToPlanner: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().widthIn(max = 1_100.dp)) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Результат", style = MaterialTheme.typography.titleLarge)
            when (result) {
                is AiGenerationResult.Text -> {
                    SelectionContainer {
                        Text(result.text, style = MaterialTheme.typography.bodyLarge)
                    }
                    OutlinedButton(onClick = {
                        Toolkit.getDefaultToolkit().systemClipboard.setContents(
                            StringSelection(result.text),
                            null
                        )
                    }) { Text("Копировать текст") }
                }

                is AiGenerationResult.Media -> {
                    Text(File(result.filePath).name, fontWeight = FontWeight.SemiBold)
                    Text(
                        result.filePath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        enabled = Desktop.isDesktopSupported(),
                        onClick = { Desktop.getDesktop().open(File(result.filePath).parentFile) }
                    ) { Text("Открыть папку") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(enabled = projectionAvailable, onClick = onShow) {
                    Text("Показать на экране")
                }
                OutlinedButton(enabled = plannerAvailable, onClick = onAddToPlanner) {
                    Text("+ В план")
                }
            }
        }
    }
}

private fun AiGenerationResult.toPlanState(): AiAssistantPlanState = when (this) {
    is AiGenerationResult.Text -> AiAssistantPlanState(
        text = text,
        mediaPath = null,
        mediaType = PresentationBackgroundType.COLOR
    )

    is AiGenerationResult.Media -> AiAssistantPlanState(
        text = "",
        mediaPath = filePath,
        mediaType = if (kind == AiGenerationKind.VIDEO) {
            PresentationBackgroundType.VIDEO
        } else {
            PresentationBackgroundType.IMAGE
        }
    )
}

private fun AiGenerationResult.plannerTitle(): String = when (this) {
    is AiGenerationResult.Text -> "ИИ: ${text.lineSequence().firstOrNull().orEmpty().take(50).ifBlank { "текст" }}"
    is AiGenerationResult.Media -> "ИИ: ${File(filePath).name}"
}

private fun AiGenerationKind.title(): String = when (this) {
    AiGenerationKind.TEXT -> "Текст"
    AiGenerationKind.IMAGE -> "Изображение"
    AiGenerationKind.VIDEO -> "Видео"
}

private fun AiImageQuality.title(): String = when (this) {
    AiImageQuality.LOW -> "Низкое"
    AiImageQuality.MEDIUM -> "Среднее"
    AiImageQuality.HIGH -> "Высокое"
}

private fun promptHint(kind: AiGenerationKind): String = when (kind) {
    AiGenerationKind.TEXT -> "Например: составь три коротких слайда объявления о молодёжной встрече"
    AiGenerationKind.IMAGE -> "Например: яркий фон 16:9 для молодёжного богослужения без текста"
    AiGenerationKind.VIDEO -> "Например: спокойные солнечные лучи над горами, плавное движение камеры"
}

private fun presets(kind: AiGenerationKind): List<Pair<String, String>> = when (kind) {
    AiGenerationKind.TEXT -> listOf(
        "Слайд объявления" to "Создай короткий текст слайда объявления для церковного служения. Заголовок и не более трёх коротких строк.",
        "План презентации" to "Составь план презентации из пяти коротких слайдов для церковного служения. Для каждого укажи заголовок и текст.",
        "Молодёжная встреча" to "Предложи яркий и доброжелательный текст приглашения на молодёжную встречу в церкви."
    )

    AiGenerationKind.IMAGE -> listOf(
        "Фон для стиха" to "Создай спокойный горизонтальный фон 16:9 для библейского стиха, без текста, с чистым центром для надписи.",
        "Молодёжный фон" to "Создай яркий современный горизонтальный фон 16:9 для молодёжного христианского служения, без текста.",
        "Поклонение" to "Создай атмосферный горизонтальный фон 16:9 для музыкального поклонения, мягкий свет, без текста."
    )

    AiGenerationKind.VIDEO -> listOf(
        "Спокойный фон" to "Медленное движение мягких лучей света на тёмно-синем фоне, бесшовное ощущение, без текста.",
        "Природа" to "Рассвет над горами, лёгкие облака, очень плавное движение камеры, без текста.",
        "Молодёжный фон" to "Абстрактные яркие цветные волны, энергичное, но не резкое движение, без текста."
    )
}

private fun String.filterBudgetCharacters(): String =
    filter { it.isDigit() || it == '.' || it == ',' }.take(12)

private fun formatUsd(value: Double): String =
    String.format(Locale.US, "\$%.4f", value)

private fun formatEditable(value: Double): String =
    String.format(Locale.US, "%.2f", value)
