package holypresenter.org.platform.api.planner

interface PlannerService {
    val state: PlannerState
    val currentPlanId: String?
    val currentPlanName: String?
    val plans: List<PlannerInfo>

    fun newPlan()
    fun openPlan(planId: String): Boolean
    fun saveAs(name: String): Boolean
    fun add(item: PlannerItem)
    fun remove(item: PlannerItem)
    fun move(fromIndex: Int, toIndex: Int)
    fun clear()
    fun setActive(index: Int)
    fun clearActive()
}