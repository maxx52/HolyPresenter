package holypresenter.org.common.module

import androidx.compose.runtime.mutableStateListOf

class ModuleManager(
    private val context: ModuleContext
) {
    private val _modules = mutableStateListOf<HolyModule>()
    val modules: List<HolyModule> = _modules

    fun register(module: HolyModule) {
        if (_modules.any { it.id == module.id }) return

        module.onLoad(context)
        _modules.add(module)
    }

    fun unregister(moduleId: String) {
        val module = _modules.firstOrNull { it.id == moduleId } ?: return
        module.onUnload()
        _modules.remove(module)
    }
}