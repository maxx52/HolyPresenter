package holypresenter.org.platform.core

import holypresenter.org.platform.api.commands.CommandHandler
import holypresenter.org.platform.api.commands.HolyCommand

class CommandBus {
    private val handlers =
        mutableMapOf<String, MutableList<(HolyCommand) -> Unit>>()

    fun <T : HolyCommand> register(
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

    fun execute(command: HolyCommand) {
        handlers[command.name]?.forEach { handler ->
            handler(command)
        }
    }
}