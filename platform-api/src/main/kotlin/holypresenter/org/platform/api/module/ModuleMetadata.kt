package holypresenter.org.platform.api.module

data class ModuleMetadata(
    val id: String,
    val name: String,
    val version: String,
    val author: String = "",
    val description: String = "",
    val minCoreVersion: String = "1.0.0",
    val dependencies: List<String> = emptyList(),
    val apiVersion: String,
    val icon: String = "📦"
)