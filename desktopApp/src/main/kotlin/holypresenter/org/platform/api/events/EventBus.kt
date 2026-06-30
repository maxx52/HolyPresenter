package holypresenter.org.platform.api.events

class EventBus : Events {
    private val listeners = mutableMapOf<String, MutableList<(HolyEvent) -> Unit>>()

    override fun subscribe(
        eventName: String,
        handler: (HolyEvent) -> Unit
    ) {
        val eventListeners = listeners.getOrPut(eventName) {
            mutableListOf()
        }
        eventListeners.add(handler)
    }

    override fun unsubscribe(eventName: String) {
        listeners.remove(eventName)
    }

    fun unsubscribe(
        eventName: String,
        listener: (HolyEvent) -> Unit
    ) {
        listeners[eventName]?.remove(listener)
    }

    override fun publish(event: HolyEvent) {
        listeners[event.name]?.forEach { listener ->
            listener(event)
        }
    }
}