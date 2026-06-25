package holypresenter.org.platform.api.module

data class ModuleState(
    val module: HolyModule,
    val status: ModuleStatus = ModuleStatus.ENABLED
)