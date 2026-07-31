package holypresenter.org.platform.api.planner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID

abstract class BasePlannerService : PlannerService {
    final override val state = PlannerState()

    private val availablePlans = mutableStateListOf<PlannerInfo>()
    private var selectedPlanId by mutableStateOf<String?>(null)
    private var selectedPlanName by mutableStateOf<String?>(null)

    final override val currentPlanId: String?
        get() = selectedPlanId

    final override val currentPlanName: String?
        get() = selectedPlanName

    final override val plans: List<PlannerInfo>
        get() = availablePlans

    final override fun newPlan() {
        state.clear()
        selectedPlanId = null
        selectedPlanName = null
        saveSession(createPlanData())
    }

    final override fun openPlan(planId: String): Boolean {
        val plan = loadPlan(planId) ?: return false
        restore(plan)
        saveSession(createPlanData())
        return true
    }

    final override fun saveAs(name: String): Boolean {
        val normalizedName = PlannerNamePolicy.normalizeUniqueName(
            name = name,
            existingPlans = availablePlans
        ) ?: return false

        selectedPlanId = UUID.randomUUID().toString()
        selectedPlanName = normalizedName
        saveCurrentState()
        refreshPlans()
        return true
    }

    final override fun renamePlan(planId: String, newName: String): Boolean {
        val normalizedName = PlannerNamePolicy.normalizeUniqueName(
            name = newName,
            existingPlans = availablePlans,
            excludedPlanId = planId
        ) ?: return false
        val plan = loadPlan(planId) ?: return false

        return runCatching {
            savePlan(plan.copy(name = normalizedName))
            if (selectedPlanId == planId) {
                selectedPlanName = normalizedName
                saveSession(createPlanData())
            }
            refreshPlans()
            true
        }.onFailure(::reportFailure).getOrDefault(false)
    }

    final override fun deletePlan(planId: String): Boolean {
        val deleted = runCatching {
            deleteStoredPlan(planId)
        }.onFailure(::reportFailure).getOrDefault(false)
        if (!deleted) return false

        if (selectedPlanId == planId) {
            state.clear()
            selectedPlanId = null
            selectedPlanName = null
            saveSession(createPlanData())
        }
        refreshPlans()
        return true
    }

    final override fun add(item: PlannerItem) {
        state.add(item)
        saveCurrentState()
    }

    final override fun remove(item: PlannerItem) {
        state.remove(item)
        saveCurrentState()
    }

    final override fun move(fromIndex: Int, toIndex: Int) {
        state.move(fromIndex, toIndex)
        saveCurrentState()
    }

    final override fun clear() {
        state.clear()
        saveCurrentState()
    }

    final override fun setActive(index: Int) {
        state.setActive(index)
        saveCurrentState()
    }

    final override fun clearActive() {
        state.clearActive()
        saveCurrentState()
    }

    protected fun initialize(currentPlan: PlanData? = null) {
        refreshPlans()
        currentPlan?.let(::restore)
    }

    protected abstract fun loadPlan(planId: String): PlanData?
    protected abstract fun savePlan(plan: PlanData)
    protected abstract fun deleteStoredPlan(planId: String): Boolean
    protected abstract fun loadPlans(): List<PlanData>
    protected open fun saveSession(plan: PlanData) = Unit
    protected open fun reportFailure(error: Throwable) {
        println("Failed to update planner: ${error.message}")
    }

    private fun restore(plan: PlanData) {
        state.clear()
        plan.items.forEach(state::add)
        plan.activeItemIndex?.let(state::setActive)
        selectedPlanId = plan.id
        selectedPlanName = plan.name
    }

    private fun saveCurrentState() {
        val plan = createPlanData()
        runCatching {
            saveSession(plan)
            if (plan.id != null && plan.name != null) savePlan(plan)
        }.onFailure(::reportFailure)
    }

    private fun createPlanData() = PlanData(
        id = selectedPlanId,
        name = selectedPlanName,
        items = state.items.toList(),
        activeItemIndex = state.activeItemIndex
    )

    private fun refreshPlans() {
        val plans = loadPlans().mapNotNull { plan ->
            val id = plan.id ?: return@mapNotNull null
            val name = plan.name?.trim()?.takeIf(String::isNotBlank)
                ?: return@mapNotNull null
            PlannerInfo(id = id, name = name)
        }.sortedBy { it.name.lowercase() }

        availablePlans.clear()
        availablePlans.addAll(plans)
    }

    protected data class PlanData(
        val id: String?,
        val name: String?,
        val items: List<PlannerItem>,
        val activeItemIndex: Int?
    )
}
