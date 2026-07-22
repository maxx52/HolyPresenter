package holypresenter.org.platform.api.planner

import androidx.compose.runtime.mutableStateListOf

class PlannerState {
    val items = mutableStateListOf<PlannerItem>()

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
}