package holypresenter.org.platform.core

import holypresenter.org.modules.presentationtest.PresentationTestModule
import holypresenter.org.modules.welcome.WelcomeModule
import holypresenter.org.modules.quickoutput.QuickOutputModule
import holypresenter.org.modules.cloudbackup.CloudBackupModule
import holypresenter.org.modules.aiassistant.AiAssistantModule
import holypresenter.org.modules.update.UpdateModule
import holypresenter.org.app.AppVersion
import holypresenter.org.platform.ai.AiAssistantStorage
import holypresenter.org.platform.ai.ComfyUiProvider
import holypresenter.org.platform.ai.DefaultAiProviderRegistry
import holypresenter.org.platform.ai.FreeBackgroundProvider
import holypresenter.org.platform.ai.FreeTemplateAiProvider
import holypresenter.org.platform.ai.OllamaProvider
import holypresenter.org.platform.ai.OpenAiApiKeyStore
import holypresenter.org.platform.ai.OpenAiProvider
import holypresenter.org.platform.api.ai.AiProviderRegistry
import holypresenter.org.platform.api.commands.CommandBus
import holypresenter.org.platform.api.events.EventBus
import holypresenter.org.platform.api.module.ModuleContext
import holypresenter.org.platform.api.module.HolyModule
import holypresenter.org.platform.api.planner.PlannerItemHandlerRegistry
import holypresenter.org.platform.api.planner.PlannerService
import holypresenter.org.platform.api.projection.ProjectionService
import holypresenter.org.platform.video.DefaultVideoPlaybackService
import holypresenter.org.platform.api.video.VideoPlaybackService
import holypresenter.org.platform.api.audio.AudioPlaybackService
import holypresenter.org.platform.audio.VlcAudioPlaybackService
import holypresenter.org.platform.api.application.ApplicationLifecycleService
import holypresenter.org.platform.application.DesktopApplicationLifecycleService
import holypresenter.org.platform.services.DefaultServiceRegistry
import holypresenter.org.platform.layout.DefaultLayoutService
import holypresenter.org.platform.layout.repository.JsonLayoutRepository
import holypresenter.org.platform.logging.StartupLog
import holypresenter.org.platform.path.DesktopPathService
import holypresenter.org.platform.backup.ZipBackupService
import holypresenter.org.platform.cloud.yandex.YandexCloudBackupService
import holypresenter.org.platform.planner.DefaultPlannerItemHandlerRegistry
import holypresenter.org.platform.planner.JsonPlannerRepository
import holypresenter.org.platform.planner.PersistentPlannerService
import holypresenter.org.platform.plugins.PluginLoader
import holypresenter.org.platform.projection.DefaultProjectionService
import holypresenter.org.platform.settings.DefaultSettingsService
import holypresenter.org.platform.settings.repository.JsonSettingsRepository
import holypresenter.org.platform.update.ApplicationUpdateService
import holypresenter.org.platform.window.DefaultWindowService
import holypresenter.org.platform.path.PathService
import java.io.File

class PlatformRuntime(
    pathService: PathService = DesktopPathService(),
    onExit: () -> Unit = {}
) {
    private val pathService =
        pathService.also { service ->
            service.ensureDirectories()
            ZipBackupService.applyPendingRestoreIfPresent(
                applicationHome = service.home,
                applicationVersion = AppVersion.VERSION
            ).onSuccess { manifest ->
                if (manifest != null) {
                    StartupLog.info(
                        "Applied pending backup created at ${manifest.createdAtEpochMillis}"
                    )
                }
            }.onFailure { error ->
                StartupLog.error("Pending backup restore failed", error)
            }
            service.ensureDirectories()
        }

    val eventBus: EventBus = EventBus()
    val commandBus = CommandBus()
    val serviceRegistry = DefaultServiceRegistry()

    private val videoPlaybackService =
        DefaultVideoPlaybackService()

    private val audioPlaybackService = VlcAudioPlaybackService()

    private val applicationLifecycleService =
        DesktopApplicationLifecycleService(onExit)

    private val projectionService =
        DefaultProjectionService(
            videoPlaybackService
        )

    private val plannerItemHandlerRegistry = DefaultPlannerItemHandlerRegistry()

    private val plannerService =
        PersistentPlannerService(
            repository =
                JsonPlannerRepository(
                    plannerDirectory = File(
                        pathService.settings,
                        "planner"
                    ),
                    legacyPlannerFile = File(
                        pathService.settings,
                        "planner.json"
                    )
                )
        )

    private val backupService = ZipBackupService(
        applicationHome = pathService.home,
        applicationVersion = AppVersion.VERSION
    )

    private val yandexCloudBackupService = YandexCloudBackupService(
        applicationHome = pathService.home,
        backupService = backupService
    )

    private val applicationUpdateService = ApplicationUpdateService(
        applicationHome = pathService.home,
        currentVersion = AppVersion.VERSION,
        onExit = onExit
    )

    private val aiProviderRegistry = DefaultAiProviderRegistry()
    private val aiAssistantStorage = AiAssistantStorage(pathService.home)
    private val openAiApiKeyStore = OpenAiApiKeyStore(pathService.home)
    private val openAiProvider = OpenAiProvider(
        apiKeyStore = openAiApiKeyStore,
        applicationHome = pathService.home
    )
    private val freeTemplateAiProvider = FreeTemplateAiProvider()
    private val ollamaProvider = OllamaProvider()
    private val freeBackgroundProvider = FreeBackgroundProvider(pathService.home)
    private val comfyUiProvider = ComfyUiProvider(pathService.home)

    val moduleRegistry = ModuleRegistry(
        context = ModuleContext(
            commands = commandBus,
            events = eventBus,
            services = serviceRegistry
        )
    )

    val layoutService = DefaultLayoutService(
        repository = JsonLayoutRepository(
            layoutDirectory = pathService.layouts
        )
    )

    val settingsService = DefaultSettingsService(
        repository = JsonSettingsRepository(
            settingsFile = File(pathService.settings, "platform.json")
        )
    )

    val windowService = DefaultWindowService(
        layoutService = layoutService
    )

    val context = PlatformContext(
        eventBus = eventBus,
        commandBus = commandBus,
        services = serviceRegistry,
        windowService = windowService,
        layoutService = layoutService,
        settingsService = settingsService
    )

    private val installedModulesDirectory = File(pathService.home, "modules")

    private val pluginLoader = PluginLoader(
        bundledModulesDirectory = pathService.modules,
        installedModulesDirectory = installedModulesDirectory
    )

    private val builtinModules = mapOf(
        "welcome" to ::WelcomeModule,
        "presentation-test" to ::PresentationTestModule,
        "quick-output" to ::QuickOutputModule,
        "cloud-backup" to { CloudBackupModule(yandexCloudBackupService) },
        "updates" to { UpdateModule(applicationUpdateService) },
        "ai-assistant" to {
            AiAssistantModule(
                storage = aiAssistantStorage,
                apiKeyStore = openAiApiKeyStore,
                freeTemplateProvider = freeTemplateAiProvider,
                ollamaProvider = ollamaProvider,
                freeBackgroundProvider = freeBackgroundProvider,
                comfyUiProvider = comfyUiProvider,
                openAiProvider = openAiProvider
            )
        }
    )
    private val builtinModuleIds = builtinModules.keys
    private val disabledModuleIds = ModulePreferences.disabledIds().toMutableSet()
    private val externalModules = linkedMapOf<String, HolyModule>()

    init {
        StartupLog.info("Disabled module ids: ${disabledModuleIds.sorted()}")
    }

    fun disableModule(moduleId: String) {
        moduleRegistry.unregister(moduleId)
        disabledModuleIds += moduleId
        ModulePreferences.setDisabled(disabledModuleIds)
    }

    fun enableBuiltinModule(moduleId: String): Boolean {
        val factory = builtinModules[moduleId] ?: return false
        disabledModuleIds -= moduleId
        ModulePreferences.setDisabled(disabledModuleIds)
        moduleRegistry.register(factory())
        return true
    }

    fun disabledBuiltinModuleIds(): Set<String> = disabledModuleIds.intersect(builtinModuleIds)

    fun disabledExternalModules(): List<HolyModule> =
        externalModules.values.filter { module ->
            module.metadata.id in disabledModuleIds
        }

    fun enableExternalModule(moduleId: String): Boolean {
        val module = externalModules[moduleId] ?: return false
        disabledModuleIds -= moduleId
        ModulePreferences.setDisabled(disabledModuleIds)
        moduleRegistry.register(module)
        return true
    }

    fun importModuleArchive(archive: File): String {
        pluginLoader.importModuleArchive(archive)
        return "Модуль добавлен. Перезапустите HolyPresenter, чтобы включить его."
    }

    fun deleteModule(moduleId: String): Boolean {
        if (moduleId in builtinModuleIds) return false
        disableModule(moduleId)
        val deleted = pluginLoader.deleteModuleArchive(moduleId)
        if (deleted) {
            externalModules.remove(moduleId)
        }
        return deleted
    }

    fun canDeleteModule(moduleId: String): Boolean = moduleId !in builtinModuleIds && pluginLoader.hasArchive(moduleId)

    private fun registerServices() {
        serviceRegistry.register(
            ApplicationLifecycleService::class,
            applicationLifecycleService
        )

        serviceRegistry.register(
            PlannerItemHandlerRegistry::class,
            plannerItemHandlerRegistry
        )

        serviceRegistry.register(
            PlannerService::class,
            plannerService
        )

        serviceRegistry.register(
            VideoPlaybackService::class,
            videoPlaybackService
        )

        serviceRegistry.register(
            AudioPlaybackService::class,
            audioPlaybackService
        )

        serviceRegistry.register(
            ProjectionService::class,
            projectionService
        )

        serviceRegistry.register(
            AiProviderRegistry::class,
            aiProviderRegistry
        )
    }

    init {
        registerServices()
        registerBuiltinModules()
        registerExternalModules()
    }

    fun start() {
        layoutService.load("Default")
        settingsService.load()
        val videoService = serviceRegistry.get(VideoPlaybackService::class)

        println(
            "Video service registered: ${videoService != null}"
        )
    }

    fun stop() {
        projectionService.close()
        videoPlaybackService.release()
        audioPlaybackService.release()
        layoutService.save()
        settingsService.save()
    }

    private fun registerBuiltinModules() {
        builtinModules.values.forEach { registerIfEnabled(it()) }
    }

    private fun registerExternalModules() {
        pluginLoader.loadModules().forEach { module ->
            externalModules[module.metadata.id] = module
            runCatching {
                registerIfEnabled(module)
            }.onFailure { error ->
                println("Failed to load module ${module.metadata.id}: ${error.message}")
            }
        }
    }

    private fun registerIfEnabled(module: HolyModule) {
        if (module.metadata.id !in disabledModuleIds) moduleRegistry.register(module)
    }
}
