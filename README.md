This is a Kotlin Multiplatform project targeting Desktop (JVM).

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Desktop app:
  - Hot reload: `./gradlew :desktopApp:hotRun --auto`
  - Standard run: `./gradlew :desktopApp:run`

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Desktop tests: `./gradlew :shared:jvmTest`
- Release checks: `./gradlew :desktopApp:test :desktopApp:verifyBundledModules`

Before delivering a build, follow the [release test checklist](./TESTING.md).

### Yandex Disk backups

The built-in cloud backup module works offline-first and stores versioned
`.holybackup` archives in the user's Yandex Disk application folder. OAuth
setup and security notes are documented in
[docs/YANDEX_DISK_BACKUP.md](./docs/YANDEX_DISK_BACKUP.md).

### ИИ-помощник

Встроенный ИИ-помощник бесплатно оформляет текст по офлайн-шаблонам, использует
локальную Qwen3 через Ollama, создаёт лёгкие цветные фоны и подключается к ComfyUI
для бесплатной нейрогенерации изображений. Облачная генерация текста, изображений
и коротких видео подключается персональным API-ключом пользователя. Перед платной
генерацией показывается стоимость, действует месячный лимит, а результаты можно
сразу вывести на экран или добавить в план служения. Подробности описаны в
[docs/AI_ASSISTANT.md](./docs/AI_ASSISTANT.md).

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
