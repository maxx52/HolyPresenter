package holypresenter.org.platform.backup

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ZipBackupServiceTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun createBackup_excludesPrivateAndHeavyDataByDefault() {
        val home = temporaryFolder.newFolder("HolyPresenter")
        home.resolve("settings/platform.json").writeFile("settings")
        home.resolve("songs/song.json").writeFile("song")
        home.resolve("media/background.png").writeFile("image")
        home.resolve("media/music.mp3").writeFile("audio")
        home.resolve("media/sermon.mp4").writeFile("video")
        home.resolve("logs/startup.log").writeFile("log")
        home.resolve("cloud/yandex-token.dpapi").writeFile("secret")
        home.resolve("modules/songs.jar").writeFile("module")

        val service = ZipBackupService(home, "test")
        val backup = service.createBackup()

        assertTrue(backup.file.isFile)
        val paths = service.inspect(backup.file).files.map { it.path }.toSet()
        assertTrue("settings/platform.json" in paths)
        assertTrue("songs/song.json" in paths)
        assertTrue("media/background.png" in paths)
        assertTrue("media/music.mp3" in paths)
        assertFalse("media/sermon.mp4" in paths)
        assertFalse(paths.any { it.startsWith("logs/") })
        assertFalse(paths.any { it.startsWith("cloud/") })
        assertFalse(paths.any { it.startsWith("modules/") })
    }

    @Test
    fun pendingRestore_isAppliedOnNextStart() {
        val home = temporaryFolder.newFolder("RestoreHome")
        val settings = home.resolve("settings/platform.json")
        settings.writeFile("before")
        val service = ZipBackupService(home, "test")
        val backup = service.createBackup()

        settings.writeText("after")
        home.resolve("settings/stale.json").writeFile("stale")
        service.scheduleRestore(backup.file)

        val restored = ZipBackupService.applyPendingRestoreIfPresent(home, "test").getOrThrow()

        assertNotNull(restored)
        assertEquals("before", settings.readText())
        assertFalse(home.resolve("settings/stale.json").exists())
        assertFalse(home.resolve("restore/pending.holybackup").exists())
    }

    @Test
    fun backupWithVideoOption_includesVideo() {
        val home = temporaryFolder.newFolder("VideoHome")
        home.resolve("media/service.mp4").writeFile("video")
        val service = ZipBackupService(home, "test")

        val backup = service.createBackup(BackupOptions(includeVideo = true))

        assertTrue(backup.manifest.files.any { it.path == "media/service.mp4" })
    }

    private fun java.io.File.writeFile(value: String) {
        parentFile?.mkdirs()
        writeText(value)
    }
}
