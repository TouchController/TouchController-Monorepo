package top.fifthlight.touchcontroller.event

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import top.fifthlight.touchcontroller.gal.PlatformWindowProvider
import top.fifthlight.touchcontroller.platform.PlatformProvider

object WindowEvents : KoinComponent {
    private val logger = LoggerFactory.getLogger(WindowEvents::class.java)
    private val platformProvider: PlatformProvider by inject()
    private lateinit var windowProvider: PlatformWindowProvider
    internal val windowWidth: Int
        get() = windowProvider.windowWidth
    internal val windowHeight: Int
        get() = windowProvider.windowHeight

    fun onWindowCreated(windowProvider: PlatformWindowProvider) {
        this.windowProvider = windowProvider
        platformProvider.load(windowProvider)
        logger.info("Loaded platform")
    }
}
