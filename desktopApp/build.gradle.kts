import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskAction
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.jar.JarFile

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "2.4.0"
}

val holyPresenterVersion = "1.0.9"

@CacheableTask
abstract class PrepareUpdateRelease : DefaultTask() {
    @get:Input
    abstract val applicationVersion: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val installerFile: RegularFileProperty

    @get:OutputFile
    abstract val manifestFile: RegularFileProperty

    @TaskAction
    fun prepare() {
        val version = applicationVersion.get()
        val installer = installerFile.get().asFile
        if (!installer.isFile) {
            throw GradleException(
                "MSI версии $version не найден: ${installer.absolutePath}"
            )
        }

        val digest = MessageDigest.getInstance("SHA-256")
        installer.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        val sha256 = digest.digest().joinToString("") { byte ->
            "%02x".format(byte)
        }
        val encodedName = URLEncoder
            .encode(installer.name, Charsets.UTF_8)
            .replace("+", "%20")
        val releaseTag = "v$version"
        val destination = manifestFile.get().asFile
        destination.parentFile.mkdirs()
        destination.writeText(
            """
            {
              "schemaVersion": 1,
              "version": "$version",
              "title": "HolyPresenter $version",
              "notes": "",
              "releasePageUrl": "https://github.com/maxx52/HolyPresenter/releases/tag/$releaseTag",
              "installer": {
                "name": "${installer.name}",
                "downloadUrl": "https://github.com/maxx52/HolyPresenter/releases/download/$releaseTag/$encodedName",
                "sizeBytes": ${installer.length()},
                "sha256": "$sha256"
              }
            }
            """.trimIndent() + "\n",
            Charsets.UTF_8
        )

        logger.lifecycle("MSI: ${installer.absolutePath}")
        logger.lifecycle("Update manifest: ${destination.absolutePath}")
        logger.lifecycle("Publish both files in GitHub Release $releaseTag")
    }
}

abstract class VerifyBundledModules : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val moduleJars: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val jars = moduleJars.files

        val unreadableJars = jars.filter { file ->
            !file.isFile || runCatching {
                JarFile(file).use { archive ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    val entries = archive.entries()
                    while (entries.hasMoreElements()) {
                        archive.getInputStream(entries.nextElement()).use { input ->
                            while (input.read(buffer) != -1) {
                                // Читаем поток до конца: так проверяется CRC архива.
                            }
                        }
                    }
                }
            }.isFailure
        }

        if (unreadableJars.isNotEmpty()) {
            throw GradleException(
                """
                В desktopApp/modules есть повреждённые JAR-архивы:
                ${unreadableJars.joinToString("\n") { "- ${it.name}" }}

                Пересоберите и скопируйте эти модули заново. Приложение не
                должно распространяться с повреждёнными встроенными модулями.
                """.trimIndent()
            )
        }

        val availableJarNames = jars
            .filter { it.isFile }
            .map { it.name.lowercase() }
            .toSet()
        val requiredModuleJars = setOf(
            "bible.jar",
            "songs.jar",
            "marketplace.jar"
        )
        val missingModuleJars = requiredModuleJars - availableJarNames

        if (missingModuleJars.isNotEmpty()) {
            throw GradleException(
                """
                В desktopApp/modules отсутствуют обязательные модули:
                ${missingModuleJars.sorted().joinToString("\n") { "- $it" }}

                Перед созданием MSI соберите соответствующие проекты. Например,
                Marketplace собирается командой:
                .\gradlew.bat :marketplace:jar

                Задача installModule скопирует marketplace.jar в
                HolyPresenter/desktopApp/modules.
                """.trimIndent()
            )
        }

        val platformUiExists =
            jars.any { file ->
                file.isFile &&
                        file.extension.equals(
                            "jar",
                            ignoreCase = true
                        ) &&
                        file.name.startsWith(
                            "platform-ui-",
                            ignoreCase = true
                        )
            }

        if (!platformUiExists) {
            throw GradleException(
                """
                В desktopApp/modules отсутствует
                platform-ui-*.jar.

                Модуль Songs без HolyPresenter Platform UI
                загрузиться не сможет.
                """.trimIndent()
            )
        }
    }
}

dependencies {
    implementation(projects.shared)
    implementation(project(":platform-api"))
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.runtime:runtime:1.11.1")
    implementation("org.jetbrains.compose.foundation:foundation:1.11.1")
    implementation("org.jetbrains.compose.material3:material3:1.9.0")
    implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
    implementation("org.jetbrains.compose.ui:ui:1.11.1")
    implementation("org.jetbrains.compose.components:components-resources:1.11.1")
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.compose.uiToolingPreview)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.tracing.desktop)
    implementation(libs.vlcj)
    implementation("net.java.dev.jna:jna:5.18.1")
    implementation("net.java.dev.jna:jna-platform:5.18.1")
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.junit)
}

val modulesSourceDirectory =
    layout.projectDirectory.dir(
        "modules"
    )

val packageResourcesDirectory =
    layout.buildDirectory.dir(
        "generated/packageResources"
    )

val bundledModuleJars =
    fileTree(
        modulesSourceDirectory.asFile
    ) {
        include("*.jar")
    }

val verifyBundledModules = tasks.register<VerifyBundledModules>(
    "verifyBundledModules"
) {
    group = "verification"
    description = "Проверяет встроенные модули HolyPresenter"

    moduleJars.from(
        bundledModuleJars
    )
}

val prepareBundledModules = tasks.register<Sync>(
    "prepareBundledModules"
) {
    group = "distribution"
    description = "Подготавливает встроенные модули HolyPresenter"

    dependsOn(verifyBundledModules)
    from(bundledModuleJars)

    into(
        packageResourcesDirectory.map { directory ->
            directory.dir("common/modules")
        }
    )
}

compose.desktop {
    application {
        mainClass = "holypresenter.org.MainKt"

        buildTypes.release.proguard {
            configurationFiles.from(
                project.file("compose-desktop.pro")
            )
        }

        /*
         * Compose будет использовать JDK,
         * на которой работает Gradle daemon.
         *
         * В текущей конфигурации проекта это
         * Amazon Corretto 21 x64.
         */
        javaHome = System.getProperty("java.home")

        /*
         * Используем функцию DSL, а не изменение
         * списка через += — так надёжнее для
         * Gradle Kotlin DSL.
         */
        jvmArgs(
            "-Dfile.encoding=UTF-8",

            /*
             * Временно отключаем CDS,
             * пока проверяем проблему запуска.
             */
            "-Xshare:off",

            /*
             * Ограничения памяти для слабых ПК.
             */
            "-Xms64m",
            "-Xmx768m"
        )

        nativeDistributions {
            targetFormats(
                TargetFormat.Exe,
                TargetFormat.Msi
            )

            packageName = "HolyPresenter"
            packageVersion = holyPresenterVersion

            /*
             * Временно включаем все модули JDK,
             * чтобы исключить ошибку урезанного runtime.
             */
            includeAllModules = true

            windows {
                menuGroup = "HolyPresenter"
                shortcut = true
                dirChooser = true

                /*
                 * Постоянный идентификатор нужен MSI для обновления уже
                 * установленной HolyPresenter поверх предыдущей версии.
                 * Его нельзя менять в следующих выпусках.
                 */
                upgradeUuid = "42472b9c-e9a3-4bb9-85d4-89a48c55bd07"

                console = false
            }
            appResourcesRootDir.set(
                packageResourcesDirectory
            )
        }
        dependsOn(
            "prepareBundledModules"
        )
    }
}

tasks.register<PrepareUpdateRelease>("prepareUpdateRelease") {
    group = "distribution"
    description = "Собирает MSI и манифест для встроенного обновления"
    dependsOn("packageMsi")

    applicationVersion.set(holyPresenterVersion)
    installerFile.set(
        layout.buildDirectory.file(
            "compose/binaries/main/msi/HolyPresenter-$holyPresenterVersion.msi"
        )
    )
    manifestFile.set(
        layout.buildDirectory.file(
            "update/holypresenter-update.json"
        )
    )
}

tasks.register("printPackagingEnvironment") {
    group = "diagnostics"
    description = "Показывает JDK и ОС, используемые для упаковки приложения"

    doLast {
        println("Packaging Java home: " + System.getProperty("java.home"))
        println("Packaging Java version: " + System.getProperty("java.version"))
        println("Packaging Java vendor: " + System.getProperty("java.vendor"))
        println("OS name: " + System.getProperty("os.name"))
        println("OS architecture: " + System.getProperty("os.arch"))
    }
}

tasks.matching { task ->
        task.name == "prepareAppResources"
    }
    .configureEach {
        dependsOn(
            prepareBundledModules
        )
    }
