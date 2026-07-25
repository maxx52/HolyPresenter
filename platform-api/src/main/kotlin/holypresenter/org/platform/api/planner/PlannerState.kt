package holypresenter.org.platform.api.planner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class PlannerState {
    val items = mutableStateListOf<PlannerItem>()
    var activeItemIndex by mutableStateOf<Int?>(null)
        private set

    fun add(item: PlannerItem) {
        items += item
    }

    fun remove(item: PlannerItem) {
        items -= item
    }

    fun move(
        fromIndex: Int,
        toIndex: Int
    ) {
        if (fromIndex == toIndex) return
        if (fromIndex !in items.indices) return
        if (toIndex !in items.indices) return

        val item = items.removeAt(fromIndex)
        items.add(toIndex, item)
    }

    fun clear() {
        items.clear()
    }

    fun setActive(index: Int) {
        if (index !in items.indices) return
        activeItemIndex = index
    }

    fun clearActive() {
        activeItemIndex = null
    }
}