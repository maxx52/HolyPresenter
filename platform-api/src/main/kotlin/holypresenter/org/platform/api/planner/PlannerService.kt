package holypresenter.org.platform.api.planner

interface PlannerService {
    val state: PlannerState

    fun add(item: PlannerItem)

    fun remove(item: PlannerItem)

    fun move(
        fromIndex: Int,
        toIndex: Int
    )

    fun clear()
}