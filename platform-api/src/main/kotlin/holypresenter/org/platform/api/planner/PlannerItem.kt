package holypresenter.org.platform.api.planner

sealed interface PlannerItem {
    val reference: PlannerReference
    val title: String

    data class Generic(
        override val reference: PlannerReference,
        override val title: String
    ) : PlannerItem
}