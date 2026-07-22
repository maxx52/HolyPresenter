package holypresenter.org.platform.api.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
object HolyIds {
    fun newId(): HolyId = HolyId(UUID.randomUUID().toString())
}