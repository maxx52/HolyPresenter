package holypresenter.org.platform.api.planner

/**
 * Обработчик элементов плана,
 * принадлежащих конкретному модулю.
 *
 * Платформа определяет обработчик
 * по PlannerReference.moduleId.
 */
interface PlannerItemHandler {
    /**
     * Идентификатор модуля-владельца.
     *
     * Должен совпадать с
     * PlannerReference.moduleId.
     */
    val moduleId: String
    /**
     * Активирует элемент плана.
     *
     * Возвращает true, если модуль
     * распознал и активировал элемент.
     */
    fun activate(item: PlannerItem): Boolean
}