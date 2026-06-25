package holypresenter.org.modules.welcome.events

import holypresenter.org.platform.api.events.HolyEvent

data class WelcomeClickedEvent(
    val message: String
) : HolyEvent {
    override val name: String = "welcome.clicked"
}