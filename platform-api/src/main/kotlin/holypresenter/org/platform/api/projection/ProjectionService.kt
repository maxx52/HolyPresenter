package holypresenter.org.platform.api.projection

import holypresenter.org.platform.api.presentation.Presentation
import holypresenter.org.platform.api.presentation.PresentationSlide

interface ProjectionService {
    val currentPresentation: Presentation?
    val currentSlideIndex: Int
    val currentSlide: PresentationSlide?

    fun present(presentation: Presentation)
    fun next()
    fun previous()
    fun goTo(index: Int)
    fun clear()
}