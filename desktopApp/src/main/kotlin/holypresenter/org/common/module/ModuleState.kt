package holypresenter.org.common.module

data class ModuleState(
    val module: HolyModule,
    val status: ModuleStatus = ModuleStatus.ENABLED
)