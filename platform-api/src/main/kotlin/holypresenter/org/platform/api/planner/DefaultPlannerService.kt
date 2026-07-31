package holypresenter.org.platform.api.planner

class DefaultPlannerService : BasePlannerService() {
    private val storedPlans = linkedMapOf<String, PlanData>()

    init {
        initialize()
    }

    override fun loadPlan(planId: String): PlanData? = storedPlans[planId]

    override fun savePlan(plan: PlanData) {
        val id = plan.id ?: return
        storedPlans[id] = plan
    }

    override fun deleteStoredPlan(planId: String): Boolean =
        storedPlans.remove(planId) != null

    override fun loadPlans(): List<PlanData> = storedPlans.values.toList()
}