package holypresenter.org.platform.api.model

@JvmInline
value class HolyId(
    val value: String
) {
    override fun toString(): String = value
}