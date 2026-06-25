package holypresenter.org.platform.core

import holypresenter.org.common.commands.CommandBus
import holypresenter.org.common.events.EventBus
import holypresenter.org.common.services.ServiceRegistry
import holypresenter.org.platform.window.WindowService

class PlatformContext(
    val eventBus: EventBus,
    val commandBus: CommandBus,
    val services: ServiceRegistry,
    val windowService: WindowService
)