package holypresenter.org.common.commands

fun interface CommandHandler<T : HolyCommand> {
    fun handle(command: T)
}