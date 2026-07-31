package holypresenter.org.platform.planner

import holypresenter.org.platform.api.planner.BasePlannerService

internal class PersistentPlannerService(
    private val repository: JsonPlannerRepository
) : BasePlannerService() {

    init {
        initialize(repository.loadCurrent().toPlanData())
    }

    override fun loadPlan(planId: String): PlanData? =
        repository.loadPlan(planId)?.toPlanData()

    override fun savePlan(plan: PlanData) {
        repository.savePlan(plan.toSnapshot())
    }

    override fun deleteStoredPlan(planId: String): Boolean =
        repository.deletePlan(planId)

    override fun loadPlans(): List<PlanData> =
        repository.listPlans().map { snapshot -> snapshot.toPlanData() }

    override fun saveSession(plan: PlanData) {
        repository.saveCurrent(plan.toSnapshot())
    }

    override fun reportFailure(error: Throwable) {
        println("Failed to update persistent planner: ${error.message}")
    }

    private fun PlannerSnapshot.toPlanData() = PlanData(
        id = id,
        name = name,
        items = items.map(PlannerItemSnapshot::toPlannerItem),
        activeItemIndex = activeItemIndex
    )

    private fun PlanData.toSnapshot() = PlannerSnapshot(
        id = id,
        name = name,
        items = items.map(PlannerItemSnapshot::from),
        activeItemIndex = activeItemIndex,
        updatedAtEpochMillis = System.currentTimeMillis()
    )
}