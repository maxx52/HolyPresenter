package holypresenter.org.platform.services

import holypresenter.org.platform.api.services.Services
import kotlin.reflect.KClass

class DefaultServiceRegistry : Services {
    private val services = mutableMapOf<KClass<*>, Any>()

    override fun <T : Any> register(
        type: KClass<T>,
        service: T
    ) {
        services[type] = service
    }

    override fun <T : Any> unregister(
        type: KClass<T>
    ) {
        services.remove(type)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(
        type: KClass<T>
    ): T? {
        return services[type] as? T
    }
}