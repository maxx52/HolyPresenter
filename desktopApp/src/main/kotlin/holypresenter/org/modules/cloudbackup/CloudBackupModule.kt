package holypresenter.org.modules.cloudbackup

import androidx.compose.runtime.Composable
import holypresenter.org.modules.cloudbackup.ui.CloudBackupWorkspace
import holypresenter.org.platform.api.module.HolyModule
import holypresenter.org.platform.api.module.ModuleMetadata
import holypresenter.org.platform.cloud.yandex.YandexCloudBackupService

class CloudBackupModule(
    private val cloudService: YandexCloudBackupService
) : HolyModule {
    override val metadata = ModuleMetadata(
        id = "cloud-backup",
        name = "Резервные копии",
        version = "1.0.0",
        apiVersion = "0.6.0",
        author = "HolyPresenter",
        description = "Backup and restore through Yandex Disk",
        icon = "☁️"
    )

    @Composable
    override fun Workspace() {
        CloudBackupWorkspace(cloudService)
    }
}

