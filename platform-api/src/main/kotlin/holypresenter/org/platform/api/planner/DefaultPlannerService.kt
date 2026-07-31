package holypresenter.org.platform.api.planner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID

class DefaultPlannerService : PlannerService {
    override val state = PlannerState()
    private val storedPlans = linkedMapOf<String, StoredPlan>()
    private val availablePlans = mutableStateListOf<PlannerInfo>()
    private var selectedPlanId by
    mutableStateOf<String?>(null)

    private var selectedPlanName by
    mutableStateOf<String?>(null)

    override val currentPlanId: String?
        get() = selectedPlanId

    override val currentPlanName: String?
        get() = selectedPlanName

    override val plans: List<PlannerInfo>
        get() = availablePlans

    override fun newPlan() {
        state.clear()
        selectedPlanId = null
        selectedPlanName = null
    }

    override fun openPlan(
        planId: String
    ): Boolean {
        val storedPlan =
            storedPlans[planId]
                ?: return false

        state.clear()

        storedPlan.items.forEach { item ->
            state.add(item)
        }

        storedPlan.activeItemIndex?.let { index ->
            state.setActive(index)
        }

        selectedPlanId = storedPlan.id
        selectedPlanName = storedPlan.name

        return true
    }

    override fun saveAs(
        name: String
    ): Boolean {
        val normalizedName = name.trim()

        if (normalizedName.isBlank()) {
            return false
        }

        if (
            availablePlans.any { plan ->
                plan.name.equals(
                    normalizedName,
                    ignoreCase = true
                )
            }
        ) {
            return false
        }

        selectedPlanId = UUID.randomUUID().toString()
        selectedPlanName = normalizedName

        saveCurrentPlan()
        refreshPlans()

        return true
    }

    override fun add(item: PlannerItem) {
        state.add(item)
        saveCurrentPlan()
    }

    override fun remove(item: PlannerItem) {
        state.remove(item)
        saveCurrentPlan()
    }

    override fun move(
        fromIndex: Int,
        toIndex: Int
    ) {
        state.move(fromIndex, toIndex)
        saveCurrentPlan()
    }

    override fun clear() {
        state.clear()
        saveCurrentPlan()
    }

    override fun setActive(index: Int) {
        state.setActive(index)
        saveCurrentPlan()
    }

    override fun clearActive() {
        state.clearActive()
        saveCurrentPlan()
    }

    private fun saveCurrentPlan() {
        val id =
            selectedPlanId ?: return

        val name =
            selectedPlanName ?: return

        storedPlans[id] =
            StoredPlan(
                id = id,
                name = name,
                items = state.items.toList(),
                activeItemIndex =
                    state.activeItemIndex
            )
    }

    private fun refreshPlans() {
        availablePlans.clear()

        availablePlans.addAll(
            storedPlans.values
                .map { storedPlan ->
                    PlannerInfo(
                        id = storedPlan.id,
                        name = storedPlan.name
                    )
                }
                .sortedBy { plan ->
                    plan.name.lowercase()
                }
        )
    }

    private data class StoredPlan(
        val id: String,
        val name: String,
        val items: List<PlannerItem>,
        val activeItemIndex: Int?
    )
}