package holypresenter.org.modules.welcome.commands

import holypresenter.org.platform.api.commands.HolyCommand

data class ShowWelcomeMessageCommand(
    val message: String
) : HolyCommand {
    override val name: String = "welcome.showMessage"
}