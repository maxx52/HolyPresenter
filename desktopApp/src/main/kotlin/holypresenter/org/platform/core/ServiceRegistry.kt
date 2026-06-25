package holypresenter.org.platform.core

import holypresenter.org.platform.api.services.HolyService
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