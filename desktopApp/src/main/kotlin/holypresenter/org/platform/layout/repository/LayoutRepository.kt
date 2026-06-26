package holypresenter.org.platform.layout.repository

import holypresenter.org.platform.layout.AppLayout

interface LayoutRepository {
    fun load(name: String): AppLayout?

    fun save(layout: AppLayout)

    fun delete(name: String)

    fun list(): List<String>
}