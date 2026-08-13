package holypresenter.org.platform.backup

import java.io.File

interface BackupService {
    val backupsDirectory: File

    fun createBackup(options: BackupOptions = BackupOptions()): LocalBackup

    fun inspect(backup: File): BackupManifest

    fun listLocalBackups(): List<LocalBackup>

    /**
     * Validates the archive and schedules it for the next application start.
     * Delayed restore prevents currently loaded services from overwriting the
     * restored settings during shutdown.
     */
    fun scheduleRestore(backup: File): BackupManifest

    fun deleteLocalBackup(backup: File): Boolean
}

