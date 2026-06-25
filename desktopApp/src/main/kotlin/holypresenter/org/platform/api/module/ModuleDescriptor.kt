package holypresenter.org.platform.api.module

data class ModuleDescriptor(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val homepage: String? = null,
    val license: String = "Apache-2.0",
    val requiredCoreVersion: String,
    val dependencies: List<String> = emptyList(),
    val optionalDependencies: List<String> = emptyList(),
    val required: Boolean = false
)