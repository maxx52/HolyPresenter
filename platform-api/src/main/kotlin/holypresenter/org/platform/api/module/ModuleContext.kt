package holypresenter.org.platform.api.module

import holypresenter.org.platform.api.commands.CommandDispatcher
import holypresenter.org.platform.api.commands.CommandRegistry
import holypresenter.org.platform.api.events.EventPublisher
import holypresenter.org.platform.api.events.EventRegistry
import holypresenter.org.platform.api.services.ServiceProvider
import holypresenter.org.platform.api.services.ServiceRegistry

class ModuleContext(
    val commands: CommandDispatcher,
    val commandRegistry: CommandRegistry,

    val events: EventPublisher,
    val eventRegistry: EventRegistry,

    val services: ServiceProvider,
    val serviceRegistry: ServiceRegistry
)