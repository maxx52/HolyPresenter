package holypresenter.org.platform.planner

import holypresenter.org.platform.api.planner.PlannerItem
import holypresenter.org.platform.api.planner.PlannerService
import holypresenter.org.platform.api.planner.PlannerState

internal class PersistentPlannerService(
    private val repository: JsonPlannerRepository
) : PlannerService {
    override val state = PlannerState()

    init {
        restore(
            snapshot = repository.load()
        )
    }

    override fun add(item: PlannerItem) {
        state.add(item)
        save()
    }

    override fun remove(item: PlannerItem) {
        state.remove(item)
        save()
    }

    override fun move(
        fromIndex: Int,
        toIndex: Int
    ) {
        state.move(
            fromIndex = fromIndex,
            toIndex = toIndex
        )
        save()
    }

    override fun clear() {
        state.clear()
        save()
    }

    override fun setActive(index: Int) {
        state.setActive(index)
        save()
    }

    override fun clearActive() {
        state.clearActive()
        save()
    }

    private fun restore(
        snapshot: PlannerSnapshot
    ) {
        snapshot.items.forEach { storedItem ->
            state.add(
                storedItem.toPlannerItem()
            )
        }

        snapshot.activeItemIndex?.let { index ->
            state.setActive(index)
        }
    }

    private fun save() {
        val snapshot = PlannerSnapshot(
            items = state.items.map(PlannerItemSnapshot::from),
            activeItemIndex = state.activeItemIndex
        )

        runCatching {
            repository.save(snapshot)
        }.onFailure { error ->
            println("Failed to save planner: + $error.message")
        }
    }
}