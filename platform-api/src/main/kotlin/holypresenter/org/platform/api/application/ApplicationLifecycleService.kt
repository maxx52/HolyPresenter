package holypresenter.org.platform.api.application

interface ApplicationLifecycleService {
    /** Starts a new HolyPresenter process and closes the current one. */
    fun restart(): Result<Unit>
}
