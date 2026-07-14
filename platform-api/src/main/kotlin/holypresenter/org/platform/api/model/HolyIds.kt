package holypresenter.org.platform.api.model

import java.util.UUID

object HolyIds {
    fun newId(): HolyId = HolyId(UUID.randomUUID().toString())
}