package holypresenter.org.platform.api.services

import kotlin.reflect.KClass

interface ServiceProvider {
    fun <T : Any> get(
        type: KClass<T>
    ): T?
}