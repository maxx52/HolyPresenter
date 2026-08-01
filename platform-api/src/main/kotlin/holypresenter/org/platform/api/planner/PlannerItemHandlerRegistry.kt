package holypresenter.org.platform.api.planner

/**
 * Платформенный реестр обработчиков
 * элементов плана.
 */
interface PlannerItemHandlerRegistry {

    /**
     * Регистрирует обработчик модуля.
     */
    fun register(handler: PlannerItemHandler)

    /**
     * Удаляет обработчик модуля.
     */
    fun unregister(moduleId: String)

    /**
     * Находит нужный модуль по moduleId
     * и передаёт ему элемент.
     */
    fun activate(item: PlannerItem): Boolean
}