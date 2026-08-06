package holypresenter.org.platform.layout

import holypresenter.org.platform.layout.repository.JsonLayoutRepository
import junit.framework.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

class JsonLayoutRepositoryTest {

    @Test
    fun saveAndLoad_returnsSavedLayout() {
        val tempDir = Files.createTempDirectory("layouts-test").toFile()
        val repository = JsonLayoutRepository(tempDir)

        val layout = AppLayout(
            name = "Default",
            windows = listOf(
                WindowLayout(
                    id = "main",
                    x = 100,
                    y = 50,
                    width = 1200,
                    height = 800,
                    isOpen = true
                )
            )
        )

        repository.save(layout)

        val loaded = repository.load("Default")

        assertEquals(layout, loaded)
    }

    @Test
    fun list_returnsSavedLayoutNames() {
        val tempDir = Files.createTempDirectory("layouts-test").toFile()
        val repository = JsonLayoutRepository(tempDir)

        repository.save(AppLayout(name = "Default"))
        repository.save(AppLayout(name = "Streaming"))

        val names = repository.list()

        assertEquals(listOf("Default", "Streaming"), names)
    }

    @Test
    fun delete_removesLayoutFile() {
        val tempDir = Files.createTempDirectory("layouts-test").toFile()
        val repository = JsonLayoutRepository(tempDir)

        repository.save(AppLayout(name = "Default"))
        repository.delete("Default")

        assertEquals(repository.load("Default"), null)
    }
}