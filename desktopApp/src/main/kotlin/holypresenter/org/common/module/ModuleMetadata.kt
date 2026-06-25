package holypresenter.org.common.module

data class ModuleMetadata(
    val id: String,
    val name: String,
    val version: String,
    val author: String = "",
    val description: String = "",
    val minCoreVersion: String = "1.0.0",
    val dependencies: List<String> = emptyList()
)