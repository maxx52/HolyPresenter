package holypresenter.org.platform.backup

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class ZipBackupService(
    private val applicationHome: File,
    private val applicationVersion: String
) : BackupService {
    override val backupsDirectory: File = File(applicationHome, BACKUPS_DIRECTORY)
    private val restoreDirectory = File(applicationHome, RESTORE_DIRECTORY)

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun createBackup(options: BackupOptions): LocalBackup {
        backupsDirectory.mkdirs()

        val createdAt = System.currentTimeMillis()
        val destination = File(
            backupsDirectory,
            "holypresenter-${BACKUP_NAME_FORMAT.format(Instant.ofEpochMilli(createdAt))}.holybackup"
        )
        val temporary = File(backupsDirectory, ".${destination.name}.tmp")
        temporary.delete()

        val files = collectFiles(options)
        val entries = files.map { file ->
            BackupFileEntry(
                path = relativePath(file),
                size = file.length(),
                sha256 = sha256(file)
            )
        }
        val manifest = BackupManifest(
            applicationVersion = applicationVersion,
            createdAtEpochMillis = createdAt,
            options = options,
            files = entries
        )

        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(temporary))).use { output ->
                // BEST_SPEED keeps backup creation responsive on older church PCs.
                output.setLevel(Deflater.BEST_SPEED)
                output.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                output.write(json.encodeToString(manifest).toByteArray(Charsets.UTF_8))
                output.closeEntry()

                files.zip(entries).forEach { (file, entry) ->
                    output.putNextEntry(ZipEntry(DATA_PREFIX + entry.path))
                    BufferedInputStream(FileInputStream(file)).use { input ->
                        input.copyTo(output)
                    }
                    output.closeEntry()
                }
            }

            moveReplacing(temporary, destination)
            return LocalBackup(destination, manifest)
        } finally {
            temporary.delete()
        }
    }

    override fun inspect(backup: File): BackupManifest {
        require(backup.isFile) { "Файл резервной копии не найден: ${backup.absolutePath}" }

        ZipFile(backup).use { archive ->
            val manifestEntry = archive.getEntry(MANIFEST_ENTRY)
                ?: error("В архиве отсутствует $MANIFEST_ENTRY")
            val manifest = archive.getInputStream(manifestEntry).bufferedReader(Charsets.UTF_8).use {
                json.decodeFromString<BackupManifest>(it.readText())
            }
            require(manifest.formatVersion <= BackupManifest.CURRENT_FORMAT_VERSION) {
                "Эта копия создана более новой версией HolyPresenter"
            }

            val archiveEntries = archive.entries().asSequence()
                .filterNot { it.isDirectory }
                .associateBy { it.name }

            manifest.files.forEach { expected ->
                val entryName = DATA_PREFIX + sanitizeRelativePath(expected.path)
                val entry = archiveEntries[entryName]
                    ?: error("В архиве отсутствует файл ${expected.path}")
                require(entry.size < 0 || entry.size == expected.size) {
                    "Размер файла ${expected.path} не совпадает с описанием копии"
                }
                val actualHash = archive.getInputStream(entry).use(::sha256)
                require(actualHash.equals(expected.sha256, ignoreCase = true)) {
                    "Файл ${expected.path} повреждён"
                }
            }
            return manifest
        }
    }

    override fun listLocalBackups(): List<LocalBackup> {
        if (!backupsDirectory.isDirectory) return emptyList()
        return backupsDirectory.listFiles { file ->
            file.isFile && file.extension.equals("holybackup", ignoreCase = true)
        }.orEmpty().mapNotNull { file ->
            runCatching { LocalBackup(file, inspect(file)) }.getOrNull()
        }.sortedByDescending { it.manifest.createdAtEpochMillis }
    }

    override fun scheduleRestore(backup: File): BackupManifest {
        val manifest = inspect(backup)
        restoreDirectory.mkdirs()
        val pending = File(restoreDirectory, PENDING_BACKUP)
        val temporary = File(restoreDirectory, ".$PENDING_BACKUP.tmp")
        backup.copyTo(temporary, overwrite = true)
        moveReplacing(temporary, pending)
        return manifest
    }

    override fun deleteLocalBackup(backup: File): Boolean {
        val canonicalDirectory = backupsDirectory.canonicalFile
        val canonicalBackup = backup.canonicalFile
        require(canonicalBackup.parentFile == canonicalDirectory) {
            "Можно удалять только локальные копии HolyPresenter"
        }
        return !canonicalBackup.exists() || canonicalBackup.delete()
    }

    private fun collectFiles(options: BackupOptions): List<File> =
        applicationHome.walkTopDown()
            .onEnter { directory ->
                directory == applicationHome || directory.name.lowercase() !in EXCLUDED_DIRECTORIES
            }
            .filter { file ->
                file.isFile && shouldInclude(file, options)
            }
            .sortedBy(::relativePath)
            .toList()

    private fun shouldInclude(file: File, options: BackupOptions): Boolean {
        val extension = file.extension.lowercase()
        if (!options.includeImages && extension in IMAGE_EXTENSIONS) return false
        if (!options.includeAudio && extension in AUDIO_EXTENSIONS) return false
        if (!options.includeVideo && extension in VIDEO_EXTENSIONS) return false
        return true
    }

    private fun relativePath(file: File): String =
        file.relativeTo(applicationHome).invariantSeparatorsPath

    companion object {
        private const val MANIFEST_ENTRY = "manifest.json"
        private const val DATA_PREFIX = "data/"
        private const val BACKUPS_DIRECTORY = "backups"
        private const val RESTORE_DIRECTORY = "restore"
        private const val PENDING_BACKUP = "pending.holybackup"

        private val BACKUP_NAME_FORMAT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd-HH-mm-ss")
            .withZone(ZoneId.systemDefault())

        private val EXCLUDED_DIRECTORIES = setOf(
            "logs",
            "backups",
            "restore",
            "modules",
            "cloud",
            "cache",
            "tmp",
            "temp"
        )

        private val PROTECTED_DURING_RESTORE = setOf(
            "logs",
            "backups",
            "restore",
            "modules",
            "cloud"
        )

        private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "gif", "bmp")
        private val AUDIO_EXTENSIONS = setOf("mp3", "wav", "ogg", "flac", "m4a", "aac", "wma")
        private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "webm", "wmv", "m4v")

        fun applyPendingRestoreIfPresent(
            applicationHome: File,
            applicationVersion: String
        ): Result<BackupManifest?> = runCatching {
            val service = ZipBackupService(applicationHome, applicationVersion)
            val pending = File(service.restoreDirectory, PENDING_BACKUP)
            if (!pending.isFile) return@runCatching null

            val manifest = service.inspect(pending)
            val staging = Files.createTempDirectory("holypresenter-restore-").toFile()
            try {
                ZipFile(pending).use { archive ->
                    manifest.files.forEach { expected ->
                        val safePath = sanitizeRelativePath(expected.path)
                        val entry = archive.getEntry(DATA_PREFIX + safePath)
                            ?: error("В архиве отсутствует файл $safePath")
                        val destination = File(staging, safePath).canonicalFile
                        require(destination.toPath().startsWith(staging.canonicalFile.toPath())) {
                            "Недопустимый путь в резервной копии: $safePath"
                        }
                        destination.parentFile?.mkdirs()
                        archive.getInputStream(entry).use { input ->
                            FileOutputStream(destination).use(input::copyTo)
                        }
                    }
                }

                val topLevelNames = manifest.files.map { entry ->
                    sanitizeRelativePath(entry.path).substringBefore('/')
                }.filterNot { it in PROTECTED_DURING_RESTORE }.toSet()

                topLevelNames.forEach { name ->
                    val current = File(applicationHome, name)
                    if (current.exists()) current.deleteRecursively()
                }

                staging.listFiles().orEmpty().forEach { source ->
                    if (source.name !in PROTECTED_DURING_RESTORE) {
                        source.copyRecursively(File(applicationHome, source.name), overwrite = true)
                    }
                }

                pending.delete()
                manifest
            } finally {
                staging.deleteRecursively()
            }
        }

        private fun sanitizeRelativePath(path: String): String {
            val normalized = path.replace('\\', '/').trimStart('/')
            require(normalized.isNotBlank()) { "Пустой путь в резервной копии" }
            require(normalized.split('/').none { it == ".." || it.isBlank() }) {
                "Недопустимый путь в резервной копии: $path"
            }
            return normalized
        }

        private fun sha256(file: File): String =
            FileInputStream(file).use(::sha256)

        private fun sha256(input: java.io.InputStream): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
            return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
        }

        private fun moveReplacing(source: File, destination: File) {
            runCatching {
                Files.move(
                    source.toPath(),
                    destination.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                )
            }.getOrElse {
                Files.move(
                    source.toPath(),
                    destination.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }
    }
}
