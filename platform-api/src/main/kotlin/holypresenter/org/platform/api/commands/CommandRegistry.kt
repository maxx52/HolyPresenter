package holypresenter.org.platform.api.commands

interface CommandRegistry {
    fun <T : HolyCommand> register(
        commandName: String,
        handler: CommandHandler<T>
    )

    fun unregister(commandName: String)
}