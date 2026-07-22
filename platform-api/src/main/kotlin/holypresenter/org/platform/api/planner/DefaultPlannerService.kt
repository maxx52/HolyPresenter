package holypresenter.org.platform.api.planner

class DefaultPlannerService : PlannerService {
    override val state = PlannerState()

    override fun add(item: PlannerItem) {
        state.add(item)
    }

    override fun remove(item: PlannerItem) {
        state.remove(item)
    }

    override fun move(
        fromIndex: Int,
        toIndex: Int
    ) {
        state.move(fromIndex, toIndex)
    }

    override fun clear() {
        state.clear()
    }
}