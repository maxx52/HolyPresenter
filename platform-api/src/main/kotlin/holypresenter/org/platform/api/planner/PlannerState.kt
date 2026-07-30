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
        val removedIndex = items.indexOf(item)

        if (removedIndex == -1) {
            return
        }

        val currentActiveIndex = activeItemIndex

        items.removeAt(removedIndex)

        activeItemIndex =
            when {
                /*
                 * Удалили активный элемент.
                 */
                currentActiveIndex == removedIndex -> null

                /*
                 * Удалили элемент, находящийся
                 * перед активным.
                 */
                currentActiveIndex != null &&
                        currentActiveIndex > removedIndex -> currentActiveIndex - 1

                else -> currentActiveIndex
            }
    }

    fun move(
        fromIndex: Int,
        toIndex: Int
    ) {
        if (fromIndex == toIndex) {
            return
        }

        if (fromIndex !in items.indices) {
            return
        }

        if (toIndex !in items.indices) {
            return
        }

        val currentActiveIndex = activeItemIndex

        val item = items.removeAt(fromIndex)
        items.add(toIndex, item)

        activeItemIndex =
            when {
                currentActiveIndex == null -> null

                /*
                 * Перемещаем сам активный элемент.
                 */
                currentActiveIndex == fromIndex -> toIndex

                /*
                 * Элемент переместили вниз.
                 * Промежуточные элементы
                 * сдвинулись на одну позицию вверх.
                 */
                fromIndex < toIndex &&
                        currentActiveIndex in
                        (fromIndex + 1)..toIndex -> currentActiveIndex - 1

                /*
                 * Элемент переместили вверх.
                 * Промежуточные элементы
                 * сдвинулись на одну позицию вниз.
                 */
                fromIndex > toIndex &&
                        currentActiveIndex in
                        toIndex until fromIndex ->
                    currentActiveIndex + 1

                else -> currentActiveIndex
            }
    }

    fun clear() {
        items.clear()
        activeItemIndex = null
    }

    fun setActive(index: Int) {
        if (index !in items.indices) {
            return
        }
        activeItemIndex = index
    }

    fun clearActive() {
        activeItemIndex = null
    }
}