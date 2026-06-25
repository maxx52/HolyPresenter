package holypresenter.org.platform.core

import androidx.compose.runtime.mutableStateListOf
import holypresenter.org.platform.api.module.HolyModule
import holypresenter.org.platform.api.module.ModuleContext

class ModuleRegistry(
    private val context: ModuleContext
) {
    private val _modules = mutableStateListOf<HolyModule>()
    val modules: List<HolyModule> = _modules

    fun register(module: HolyModule) {
        if (_modules.any { it.metadata.id == module.metadata.id }) return

        module.onInstall(context)
        module.onLoad(context)
        module.onEnable(context)

        _modules += module
    }

    fun unregister(moduleId: String) {
        val module = _modules.firstOrNull {
            it.metadata.id == moduleId
        } ?: return

        module.onDisable()
        module.onUnload()

        _modules -= module
    }
}