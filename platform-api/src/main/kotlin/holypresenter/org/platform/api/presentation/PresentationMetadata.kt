package holypresenter.org.platform.api.presentation

data class PresentationMetadata(
    val title: String,
    val author: String? = null,
    val source: String? = null,
    val copyright: String? = null,
    val language: String? = null,
    val tags: List<String> = emptyList()
)