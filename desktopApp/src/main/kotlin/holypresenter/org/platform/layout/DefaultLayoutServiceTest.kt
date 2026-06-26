package holypresenter.org.platform.layout

import holypresenter.org.platform.layout.repository.InMemoryLayoutRepository
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DefaultLayoutServiceTest {

    @Test
    fun updateWindow_addsWindowToCurrentLayout() {
        val repository = InMemoryLayoutRepository()
        val service = DefaultLayoutService(repository)

        service.updateWindow(
            WindowLayout(
                id = "main",
                x = 100,
                y = 50,
                width = 1200,
                height = 800,
                isOpen = true
            )
        )

        val window = service.window("main")

        assertEquals("main", window?.id)
        assertEquals(100, window?.x)
        assertEquals(50, window?.y)
        assertEquals(1200, window?.width)
        assertEquals(800, window?.height)
        assertEquals(true, window?.isOpen)
    }

    @Test
    fun removeWindow_removesWindowFromCurrentLayout() {
        val repository = InMemoryLayoutRepository()
        val service = DefaultLayoutService(repository)

        service.updateWindow(
            WindowLayout(id = "projector")
        )

        service.removeWindow("projector")

        assertNull(service.window("projector"))
    }
}