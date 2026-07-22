package holypresenter.org.platform.api.model

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class HolyId(
    val value: String
) {
    override fun toString(): String = value
}