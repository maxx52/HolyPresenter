package holypresenter.org.platform.path

import java.io.File

interface PathService {
    val home: File
    val modules: File
    val settings: File
    val layouts: File
    val logs: File
    fun ensureDirectories()
}