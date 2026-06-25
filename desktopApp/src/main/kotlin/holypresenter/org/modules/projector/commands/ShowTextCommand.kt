package holypresenter.org.modules.projector.commands

import holypresenter.org.platform.api.commands.HolyCommand

data class ShowTextCommand(
    val text: String
) : HolyCommand {
    override val name: String = "projector.showText"
}