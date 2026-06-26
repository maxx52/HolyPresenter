package holypresenter.org.platform.api.module

import holypresenter.org.platform.api.commands.CommandDispatcher
import holypresenter.org.platform.api.events.EventPublisher
import holypresenter.org.platform.api.services.ServiceProvider

class ModuleContext(
    val events: EventPublisher,
    val commands: CommandDispatcher,
    val services: ServiceProvider
)