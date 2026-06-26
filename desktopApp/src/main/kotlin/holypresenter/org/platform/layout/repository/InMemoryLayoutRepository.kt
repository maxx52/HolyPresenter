package holypresenter.org.platform.layout.repository

import holypresenter.org.platform.layout.AppLayout

class InMemoryLayoutRepository : LayoutRepository {
    private val layouts = mutableMapOf<String, AppLayout>()

    override fun load(name: String): AppLayout? =
        layouts[name]

    override fun save(layout: AppLayout) {
        layouts[layout.name] = layout
    }

    override fun delete(name: String) {
        layouts.remove(name)
    }

    override fun list(): List<String> =
        layouts.keys.sorted()
}