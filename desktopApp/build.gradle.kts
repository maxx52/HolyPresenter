import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.tasks.Sync

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "2.4.0"
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

val prepareBundledModules by tasks.registering(
    Sync::class
) {
    group = "distribution"
    description = "Подготавливает встроенные модули HolyPresenter"

    from(modulesSourceDirectory) {
        include("*.jar")
    }

    into(
        packageResourcesDirectory.map { directory ->
            directory.dir(
                "common/modules"
            )
        }
    )

    doFirst {
        val modulesDirectory = modulesSourceDirectory.asFile
        val songsJar = File(modulesDirectory, "songs.jar")

        require(songsJar.isFile) {
            """
            Не найден модуль Songs:

            ${songsJar.absolutePath}

            Сначала соберите HolyPresenter-Songs,
            чтобы задача installModule скопировала songs.jar.
            """.trimIndent()
        }

        val platformUiExists =
            modulesDirectory
                .listFiles()
                ?.any { file ->
                    file.isFile &&
                        file.extension.equals(
                            "jar",
                            ignoreCase = true
                        ) &&
                        file.name.startsWith(
                            "platform-ui-"
                        )
                }
                ?: false

        require(platformUiExists) {
            """
            В папке модулей отсутствует platform-ui-*.jar:

            ${modulesDirectory.absolutePath}

            Модуль Songs без HolyPresenter Platform UI
            загрузиться не сможет.
            """.trimIndent()
        }
    }
}

compose.desktop {
    application {
        mainClass = "holypresenter.org.MainKt"

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
            packageVersion = "1.0.1"

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
                 * Временная диагностическая консоль.
                 */
                console = true
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