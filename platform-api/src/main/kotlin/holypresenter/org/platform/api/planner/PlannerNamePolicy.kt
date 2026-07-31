package holypresenter.org.platform.api.planner

object PlannerNamePolicy {
    fun normalizeUniqueName(
        name: String,
        existingPlans: List<PlannerInfo>,
        excludedPlanId: String? = null
    ): String? {
        val normalizedName = name.trim()

        if (normalizedName.isBlank()) {
            return null
        }

        val duplicateExists =
            existingPlans.any { plan ->
                plan.id != excludedPlanId &&
                        plan.name.equals(
                            other = normalizedName,
                            ignoreCase = true
                        )
            }

        if (duplicateExists) {
            return null
        }
        return normalizedName
    }
}