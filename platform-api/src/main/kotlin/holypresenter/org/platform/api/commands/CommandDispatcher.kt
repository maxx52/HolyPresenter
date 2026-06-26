package holypresenter.org.platform.api.commands

interface CommandDispatcher {
    fun execute(command: HolyCommand)
}