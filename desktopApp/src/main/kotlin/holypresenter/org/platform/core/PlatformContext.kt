package holypresenter.org.platform.core

import holypresenter.org.platform.api.commands.CommandBus
import holypresenter.org.platform.api.events.EventBus
import holypresenter.org.platform.api.services.ServiceRegistry
import holypresenter.org.platform.layout.LayoutService
import holypresenter.org.platform.window.WindowService

class PlatformContext(
    val eventBus: EventBus,
    val commandBus: CommandBus,
    val services: ServiceRegistry,
    val windowService: WindowService,
    val layoutService: LayoutService
)