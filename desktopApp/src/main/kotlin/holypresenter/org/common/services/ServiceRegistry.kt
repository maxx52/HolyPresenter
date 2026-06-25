package holypresenter.org.common.services

import kotlin.reflect.KClass

class ServiceRegistry {
    private val services = mutableMapOf<KClass<*>, HolyService>()

    fun <T : HolyService> register(
        type: KClass<T>,
        service: T
    ) {
        services[type] = service
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : HolyService> get(
        type: KClass<T>
    ): T? {
        return services[type] as? T
    }
}