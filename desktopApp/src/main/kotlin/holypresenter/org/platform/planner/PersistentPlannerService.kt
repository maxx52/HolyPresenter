package holypresenter.org.platform.planner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import holypresenter.org.platform.api.planner.PlannerInfo
import holypresenter.org.platform.api.planner.PlannerItem
import holypresenter.org.platform.api.planner.PlannerService
import holypresenter.org.platform.api.planner.PlannerState
import java.util.UUID

internal class PersistentPlannerService(
    private val repository: JsonPlannerRepository
) : PlannerService {

    override val state = PlannerState()

    private val availablePlans =
        mutableStateListOf<PlannerInfo>()

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

    init {
        refreshPlans()

        restore(
            snapshot = repository.loadCurrent()
        )
    }

    override fun newPlan() {
        state.clear()

        selectedPlanId = null
        selectedPlanName = null

        saveCurrentSession()
    }

    override fun openPlan(
        planId: String
    ): Boolean {
        val snapshot =
            repository.loadPlan(planId)
                ?: return false

        restore(snapshot)
        saveCurrentSession()

        return true
    }

    override fun saveAs(
        name: String
    ): Boolean {
        val normalizedName = name.trim()

        if (normalizedName.isBlank()) {
            return false
        }

        val duplicateName =
            availablePlans.any { plan ->
                plan.name.equals(
                    other = normalizedName,
                    ignoreCase = true
                )
            }

        if (duplicateName) {
            return false
        }

        selectedPlanId =
            UUID.randomUUID().toString()

        selectedPlanName =
            normalizedName

        saveCurrentState()
        refreshPlans()

        return true
    }

    override fun add(
        item: PlannerItem
    ) {
        state.add(item)
        saveCurrentState()
    }

    override fun remove(
        item: PlannerItem
    ) {
        state.remove(item)
        saveCurrentState()
    }

    override fun move(
        fromIndex: Int,
        toIndex: Int
    ) {
        state.move(
            fromIndex = fromIndex,
            toIndex = toIndex
        )

        saveCurrentState()
    }

    override fun clear() {
        state.clear()
        saveCurrentState()
    }

    override fun setActive(
        index: Int
    ) {
        state.setActive(index)
        saveCurrentState()
    }

    override fun clearActive() {
        state.clearActive()
        saveCurrentState()
    }

    private fun restore(
        snapshot: PlannerSnapshot
    ) {
        state.clear()

        snapshot.items.forEach { storedItem ->
            state.add(
                storedItem.toPlannerItem()
            )
        }

        snapshot.activeItemIndex?.let { index ->
            state.setActive(index)
        }

        selectedPlanId = snapshot.id
        selectedPlanName = snapshot.name
    }

    private fun saveCurrentState() {
        val snapshot = createSnapshot()

        runCatching {
            /*
             * current.json хранит текущую сессию,
             * включая ещё не именованный план.
             */
            repository.saveCurrent(snapshot)

            /*
             * Именованный план дополнительно
             * сохраняется в каталоге plans.
             */
            if (
                snapshot.id != null &&
                snapshot.name != null
            ) {
                repository.savePlan(snapshot)
            }
        }.onFailure { error ->
            println(
                "Failed to save planner: " +
                        error.message
            )
        }
    }

    private fun saveCurrentSession() {
        runCatching {
            repository.saveCurrent(
                createSnapshot()
            )
        }.onFailure { error ->
            println(
                "Failed to save current planner session: " +
                        error.message
            )
        }
    }

    private fun createSnapshot():
            PlannerSnapshot =
        PlannerSnapshot(
            id = selectedPlanId,
            name = selectedPlanName,
            items = state.items.map { item ->
                PlannerItemSnapshot.from(item)
            },
            activeItemIndex =
                state.activeItemIndex,
            updatedAtEpochMillis =
                System.currentTimeMillis()
        )

    private fun refreshPlans() {
        availablePlans.clear()

        val loadedPlans =
            repository
                .listPlans()
                .mapNotNull { snapshot ->
                    val id =
                        snapshot.id
                            ?: return@mapNotNull null

                    val name =
                        snapshot.name
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?: return@mapNotNull null

                    PlannerInfo(
                        id = id,
                        name = name
                    )
                }
                .sortedBy { plan ->
                    plan.name.lowercase()
                }

        availablePlans.addAll(
            loadedPlans
        )
    }
}