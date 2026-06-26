package holypresenter.org.platform.api.services

import kotlin.reflect.KClass

interface ServiceProvider {
    fun <T : HolyService> get(type: KClass<T>): T?
}