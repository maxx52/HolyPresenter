package holypresenter.org.platform.api.module

import holypresenter.org.platform.api.commands.Commands
import holypresenter.org.platform.api.events.Events
import holypresenter.org.platform.api.services.Services

class ModuleContext(
    val commands: Commands,
    val events: Events,
    val services: Services
)