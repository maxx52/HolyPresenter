package holypresenter.org.platform.api.commands

fun interface CommandHandler<T : HolyCommand> {
    fun handle(command: T)
}