package holypresenter.org.platform.api.events

interface EventPublisher {
    fun publish(event: HolyEvent)
}