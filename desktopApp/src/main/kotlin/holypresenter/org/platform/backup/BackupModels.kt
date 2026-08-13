package holypresenter.org.platform.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupOptions(
    val includeImages: Boolean = true,
    val includeAudio: Boolean = true,
    val includeVideo: Boolean = false
)

@Serializable
data class BackupFileEntry(
    val path: String,
    val size: Long,
    val sha256: String
)

@Serializable
data class BackupManifest(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val applicationVersion: String,
    val createdAtEpochMillis: Long,
    val options: BackupOptions,
    val files: List<BackupFileEntry>
) {
    companion object {
        const val CURRENT_FORMAT_VERSION = 1
    }
}

data class LocalBackup(
    val file: java.io.File,
    val manifest: BackupManifest
)

