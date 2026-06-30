package holypresenter.org.platform.api.events

interface EventRegistry {
    fun subscribe(
        eventName: String,
        handler: (HolyEvent) -> Unit
    )

    fun unsubscribe(eventName: String)
}