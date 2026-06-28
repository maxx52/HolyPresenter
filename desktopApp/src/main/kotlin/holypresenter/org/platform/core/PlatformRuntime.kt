package holypresenter.org.platform.core

import holypresenter.org.modules.welcome.WelcomeModule
import holypresenter.org.platform.api.commands.CommandBus
import holypresenter.org.platform.api.events.EventBus
import holypresenter.org.platform.api.module.ModuleContext
import holypresenter.org.platform.api.services.ServiceRegistry
import holypresenter.org.platform.layout.DefaultLayoutService
import holypresenter.org.platform.layout.repository.JsonLayoutRepository
import holypresenter.org.platform.plugins.PluginLoader
import holypresenter.org.platform.settings.DefaultSettingsService
import holypresenter.org.platform.settings.repository.JsonSettingsRepository
import holypresenter.org.platform.window.DefaultWindowService
import java.io.File

class PlatformRuntime(
    rootDirectory: File = File("HolyPresenter")
) {
    val eventBus = EventBus()
    val commandBus = CommandBus()
    val services = ServiceRegistry()

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
        services = services,
        windowService = windowService,
        layoutService = layoutService,
        settingsService = settingsService
    )

    val moduleRegistry = ModuleRegistry(
        context = ModuleContext(
            platform = context
        )
    )

    private val pluginLoader = PluginLoader(
        modulesDirectory = File(rootDirectory, "modules")
    )

    init {
        registerBuiltinModules()
        registerExternalModules()
    }

    fun start() {
        layoutService.load("Default")
        settingsService.load()
    }

    fun stop() {
        layoutService.save()
        settingsService.save()
    }

    private fun registerBuiltinModules() {
        moduleRegistry.register(WelcomeModule())
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