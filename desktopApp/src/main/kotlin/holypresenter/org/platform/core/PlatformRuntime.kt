package holypresenter.org.platform.core

import holypresenter.org.modules.presentationtest.PresentationTestModule
import holypresenter.org.modules.welcome.WelcomeModule
import holypresenter.org.platform.api.commands.CommandBus
import holypresenter.org.platform.api.events.EventBus
import holypresenter.org.platform.api.module.ModuleContext
import holypresenter.org.platform.api.planner.PlannerService
import holypresenter.org.platform.api.projection.ProjectionService
import holypresenter.org.platform.video.DefaultVideoPlaybackService
import holypresenter.org.platform.api.video.VideoPlaybackService
import holypresenter.org.platform.services.DefaultServiceRegistry
import holypresenter.org.platform.layout.DefaultLayoutService
import holypresenter.org.platform.layout.repository.JsonLayoutRepository
import holypresenter.org.platform.path.DesktopPathService
import holypresenter.org.platform.planner.JsonPlannerRepository
import holypresenter.org.platform.planner.PersistentPlannerService
import holypresenter.org.platform.plugins.PluginLoader
import holypresenter.org.platform.projection.DefaultProjectionService
import holypresenter.org.platform.settings.DefaultSettingsService
import holypresenter.org.platform.settings.repository.JsonSettingsRepository
import holypresenter.org.platform.window.DefaultWindowService
import java.io.File

class PlatformRuntime(
    rootDirectory: File = File("HolyPresenter")
) {
    val eventBus: EventBus = EventBus()
    val commandBus = CommandBus()
    private val pathService = DesktopPathService()
    val serviceRegistry = DefaultServiceRegistry()
    private val videoPlaybackService = DefaultVideoPlaybackService()
    private val projectionService = DefaultProjectionService(videoPlaybackService)

    private val plannerService =
        PersistentPlannerService(
            repository =
                JsonPlannerRepository(
                    plannerDirectory = File(
                        rootDirectory,
                        "settings/planner"
                    ),
                    legacyPlannerFile = File(
                        rootDirectory,
                        "settings/planner.json"
                    )
                )
        )

    val moduleRegistry = ModuleRegistry(
        context = ModuleContext(
            commands = commandBus,
            events = eventBus,
            services = serviceRegistry
        )
    )

    val layoutService = DefaultLayoutService(
        repository = JsonLayoutRepository(
            layoutDirectory = File(rootDirectory, "layouts")
        )
    )

    val settingsService = DefaultSettingsService(
        repository = JsonSettingsRepository(
            settingsFile = File(rootDirectory, "settings/platform.json")
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

    private val pluginLoader = PluginLoader(
        pathService.modules
    )

    private fun registerServices() {
        serviceRegistry.register(
            PlannerService::class,
            plannerService
        )

        serviceRegistry.register(
            VideoPlaybackService::class,
            videoPlaybackService
        )

        serviceRegistry.register(
            ProjectionService::class,
            projectionService
        )
    }

    init {
        registerServices()
        pathService.ensureDirectories()
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
        layoutService.save()
        settingsService.save()
    }

    private fun registerBuiltinModules() {
        moduleRegistry.register(WelcomeModule())
        moduleRegistry.register(PresentationTestModule())
    }

    private fun registerExternalModules() {
        pluginLoader.loadModules().forEach { module ->
            runCatching {
                moduleRegistry.register(module)
            }.onFailure { error ->
                println("Failed to load module ${module.metadata.id}: ${error.message}")
            }
        }
    }
}