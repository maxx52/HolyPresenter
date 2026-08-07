package holypresenter.org.platform.logging

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object StartupLog {
    private const val MAX_LOG_SIZE_BYTES = 2L * 1024L * 1024L

    private val lock = ReentrantLock()

    private val timestampFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    private val logFile: File? by lazy {
        resolveLogFile()
    }

    fun begin() {
        rotateIfNecessary()

        info(
            "=================================================="
        )
        info("HolyPresenter startup")
        info(
            "Log file: ${logFile?.absolutePath ?: "unavailable"}"
        )
        info(
            "Java version: ${System.getProperty("java.version")}"
        )
        info(
            "Java vendor: ${System.getProperty("java.vendor")}"
        )
        info(
            "Java home: ${System.getProperty("java.home")}"
        )
        info(
            "OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")}"
        )
        info(
            "Architecture: ${System.getProperty("os.arch")}"
        )
        info(
            "User directory: ${System.getProperty("user.dir")}"
        )
        info(
            "User home: ${System.getProperty("user.home")}"
        )
        info(
            "Compose resources: " + (System.getProperty("compose.application.resources.dir") ?: "not set")
        )
    }

    fun info(message: String) {
        write(
            level = "INFO",
            message = message
        )
    }

    fun warning(message: String) {
        write(
            level = "WARN",
            message = message
        )
    }

    fun error(
        message: String,
        throwable: Throwable? = null
    ) {
        write(
            level = "ERROR",
            message = message
        )

        if (throwable != null) {
            writeThrowable(throwable)
        }
    }

    fun path(): String? = logFile?.absolutePath

    private fun write(
        level: String,
        message: String
    ) {
        val line =
            buildString {
                append(
                    LocalDateTime.now().format(timestampFormatter)
                )
                append(" [")
                append(level)
                append("] [")
                append(Thread.currentThread().name)
                append("] ")
                append(message)
            }
        println(line)

        val file = logFile ?: return

        lock.withLock {
            runCatching {
                file.appendText(
                    text = "$line${System.lineSeparator()}",
                    charset = Charsets.UTF_8
                )
            }.onFailure { error ->
                System.err.println(
                    "Unable to write startup log: " +
                            error.message
                )
            }
        }
    }

    private fun writeThrowable(
        throwable: Throwable
    ) {
        val stackTrace =
            StringWriter().use { buffer ->
                PrintWriter(buffer).use { writer ->
                    throwable.printStackTrace(writer)
                }
                buffer.toString()
            }

        stackTrace
            .lineSequence()
            .forEach { line ->
                write(
                    level = "ERROR",
                    message = line
                )
            }
    }

    private fun rotateIfNecessary() {
        val file = logFile ?: return

        lock.withLock {
            runCatching {
                if (
                    file.isFile && file.length() > MAX_LOG_SIZE_BYTES
                ) {
                    val previousLog =
                        File(
                            file.parentFile,
                            "startup.previous.log"
                        )

                    if (previousLog.exists()) {
                        previousLog.delete()
                    }

                    file.copyTo(
                        target = previousLog,
                        overwrite = true
                    )

                    file.writeText(
                        text = "",
                        charset = Charsets.UTF_8
                    )
                }
            }
        }
    }

    private fun resolveLogFile(): File? {
        val localAppData =
            System.getenv("LOCALAPPDATA")
                ?.takeIf { path ->
                    path.isNotBlank()
                }
                ?.let(::File)

        val candidates =
            listOfNotNull(
                localAppData
                    ?.resolve("HolyPresenter")
                    ?.resolve("logs")
                    ?.resolve("startup.log"),

                File(
                    System.getProperty("user.home"),
                    ".holypresenter/logs/startup.log"
                ),

                File(
                    System.getProperty("java.io.tmpdir"),
                    "HolyPresenter/logs/startup.log"
                )
            ).distinctBy { file ->
                file.absolutePath
            }

        return candidates.firstNotNullOfOrNull { candidate ->
            runCatching {
                candidate.parentFile?.mkdirs()

                if (!candidate.exists()) {
                    candidate.createNewFile()
                }

                candidate.takeIf { file ->
                    file.isFile && file.canWrite()
                }
            }.getOrNull()
        }
    }
}