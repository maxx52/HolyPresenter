package holypresenter.org.modules.welcome.commands

import holypresenter.org.common.commands.HolyCommand

data class ShowWelcomeMessageCommand(
    val message: String
) : HolyCommand {
    override val name: String = "welcome.showMessage"
}