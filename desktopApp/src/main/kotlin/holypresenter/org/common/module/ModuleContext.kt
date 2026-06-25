package holypresenter.org.common.module

import holypresenter.org.common.events.EventBus

class ModuleContext(
    val appName: String = "HolyPresenter",
    val eventBus: EventBus
)