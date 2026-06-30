package holypresenter.org.platform.api.services

import kotlin.reflect.KClass

interface ServiceRegistry {
    fun <T : Any> register(
        type: KClass<T>,
        service: T
    )

    fun <T : Any> unregister(
        type: KClass<T>
    )
}