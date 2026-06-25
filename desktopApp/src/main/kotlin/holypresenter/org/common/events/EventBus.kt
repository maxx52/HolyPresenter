package holypresenter.org.common.events

class EventBus {
    private val listeners = mutableMapOf<String, MutableList<(HolyEvent) -> Unit>>()

    fun subscribe(
        eventName: String,
        listener: (HolyEvent) -> Unit
    ) {
        val eventListeners = listeners.getOrPut(eventName) {
            mutableListOf()
        }
        eventListeners.add(listener)
    }

    fun unsubscribe(
        eventName: String,
        listener: (HolyEvent) -> Unit
    ) {
        listeners[eventName]?.remove(listener)
    }

    fun publish(event: HolyEvent) {
        listeners[event.name]?.forEach { listener ->
            listener(event)
        }
    }
}