package holypresenter.org.platform.api.presentation.theme

data class PresentationTheme(
    val background: PresentationBackground,
    val textStyle: PresentationTextStyle = PresentationTextStyle(),
    val overlay: PresentationOverlay = PresentationOverlay()
)