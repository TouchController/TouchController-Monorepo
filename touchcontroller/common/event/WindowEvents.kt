package top.fifthlight.touchcontroller.common.event

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import top.fifthlight.touchcontroller.common.gal.PlatformWindowProvider
import top.fifthlight.touchcontroller.common.platform.PlatformProvider
import top.fifthlight.touchcontroller.proxy.message.InitializeMessage
import java.util.concurrent.Executor

object WindowEvents : KoinComponent {
    private val logger = LoggerFactory.getLogger(WindowEvents::class.java)
    private val platformProvider: PlatformProvider by inject()
    private lateinit var windowProvider: PlatformWindowProvider
    val windowWidth: Int
        get() = windowProvider.windowWidth
    val windowHeight: Int
        get() = windowProvider.windowHeight

    private fun interface MainThreadDispatcher : Executor

    private class EmptyDispatcher : MainThreadDispatcher {
        override fun execute(command: Runnable) = command.run()
    }

    private class IxerisDispatcher(clazz: Class<*>) : MainThreadDispatcher {
        init {
            require(clazz.canonicalName == "me.decce.ixeris.api.IxerisApi") { "Bad ixeris API class" }
        }

        private val instance = clazz.getMethod("getInstance").invoke(null)
        private val runLaterOnMainThreadMethod = clazz.getMethod("runLaterOnMainThread", Runnable::class.java)

        override fun execute(command: Runnable) {
            runLaterOnMainThreadMethod.invoke(instance, command)
        }
    }

    private val mainThreadDispatcher by lazy {
        try {
            IxerisDispatcher(Class.forName("me.decce.ixeris.api.IxerisApi"))
        } catch (_: ClassNotFoundException) {
            EmptyDispatcher()
        }
    }

    fun onWindowCreated(windowProvider: PlatformWindowProvider) {
        this.windowProvider = windowProvider
        mainThreadDispatcher.execute {
            platformProvider.load(windowProvider)
            platformProvider.platform?.sendEvent(InitializeMessage)
            logger.info("Loaded platform on thread ${Thread.currentThread().name}")
        }
    }
}
