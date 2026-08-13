package holypresenter.org.platform.cloud.yandex

import holypresenter.org.platform.backup.BackupManifest
import holypresenter.org.platform.backup.BackupOptions
import holypresenter.org.platform.backup.BackupService
import holypresenter.org.platform.backup.LocalBackup
import java.awt.Desktop
import java.io.File
import java.net.URI

class YandexCloudBackupService(
    applicationHome: File,
    private val backupService: BackupService,
    private val config: YandexCloudConfig = YandexCloudConfig(applicationHome),
    private val tokenStore: DpapiYandexTokenStore = DpapiYandexTokenStore(applicationHome),
    private val oauthClient: YandexOAuthClient = YandexOAuthClient()
) {
    private var pendingAuthorization: PendingYandexAuthorization? = null

    fun clientId(): String = config.clientId()

    fun saveClientId(clientId: String) = config.saveClientId(clientId)

    fun isConnected(): Boolean = tokenStore.load() != null

    fun beginAuthorization(): PendingYandexAuthorization {
        val pending = oauthClient.begin(clientId())
        check(Desktop.isDesktopSupported()) { "Не удалось открыть системный браузер" }
        Desktop.getDesktop().browse(URI.create(pending.authorizationUrl))
        pendingAuthorization = pending
        return pending
    }

    fun finishAuthorization(code: String) {
        val pending = pendingAuthorization
            ?: error("Сначала нажмите «Подключить Яндекс Диск»")
        val token = oauthClient.exchangeCode(clientId(), code, pending)
        tokenStore.save(token)
        pendingAuthorization = null
    }

    fun disconnect() {
        pendingAuthorization = null
        tokenStore.clear()
    }

    fun createAndUpload(options: BackupOptions): LocalBackup {
        val backup = backupService.createBackup(options)
        diskClient().upload(backup.file)
        return backup
    }

    fun listRemoteBackups(): List<YandexRemoteBackup> = diskClient().listBackups()

    fun downloadAndScheduleRestore(remote: YandexRemoteBackup): BackupManifest {
        val destination = File(backupService.backupsDirectory, remote.name)
        diskClient().download(remote, destination)
        return backupService.scheduleRestore(destination)
    }

    fun deleteRemoteBackup(remote: YandexRemoteBackup) = diskClient().delete(remote)

    private fun diskClient(): YandexDiskClient = YandexDiskClient(validAccessToken())

    private fun validAccessToken(): String {
        val stored = tokenStore.load()
            ?: error("Яндекс Диск не подключён")
        if (!stored.isExpiring()) return stored.accessToken

        val refreshToken = stored.refreshToken
            ?: error("Срок авторизации истёк. Подключите Яндекс Диск повторно")
        val refreshed = oauthClient.refresh(clientId(), refreshToken)
        val complete = refreshed.copy(
            refreshToken = refreshed.refreshToken ?: refreshToken
        )
        tokenStore.save(complete)
        return complete.accessToken
    }
}
