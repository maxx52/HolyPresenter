package holypresenter.org.platform.api.commands

class CommandBus : Commands {
    private val handlers =
        mutableMapOf<String, MutableList<(HolyCommand) -> Unit>>()

    override fun <T : HolyCommand> register(
        commandName: String,
        handler: CommandHandler<T>
    ) {
        val list = handlers.getOrPut(commandName) {
            mutableListOf()
        }

        list.add { command ->
            @Suppress("UNCHECKED_CAST")
            handler.handle(command as T)
        }
    }

    override fun unregister(commandName: String) {
        handlers.remove(commandName)
    }

    override fun execute(command: HolyCommand) {
        handlers[command.name]?.forEach { handler ->
            handler(command)
        }
    }
}