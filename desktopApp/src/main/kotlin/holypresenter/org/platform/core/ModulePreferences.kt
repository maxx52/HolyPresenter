package holypresenter.org.platform.core

import java.util.prefs.Preferences

internal object ModulePreferences {
    private val preferences = Preferences.userRoot().node("org/holypresenter/modules")

    fun disabledIds(): Set<String> = preferences.get("disabled", "").split(',').filter(String::isNotBlank).toSet()
    fun setDisabled(ids: Set<String>) = preferences.put("disabled", ids.sorted().joinToString(","))
}
