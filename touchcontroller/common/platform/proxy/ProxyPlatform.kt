package top.fifthlight.touchcontroller.common.platform.proxy

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.fifthlight.touchcontroller.common.platform.LargeMessageWrappedPlatform
import top.fifthlight.touchcontroller.proxy.message.ProxyMessage
import top.fifthlight.touchcontroller.proxy.server.LauncherSocketProxyServer

class ProxyPlatform(scope: CoroutineScope, private val proxy: LauncherSocketProxyServer) :
    LargeMessageWrappedPlatform() {
    init {
        scope.launch {
            proxy.start()
        }
    }

    override fun pollSmallEvent(): ProxyMessage? = proxy.receive()

    override fun sendSmallEvent(message: ProxyMessage) {
        // UDP backend don't support sending message
    }
}
