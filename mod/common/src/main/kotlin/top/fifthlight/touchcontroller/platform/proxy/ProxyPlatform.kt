package top.fifthlight.touchcontroller.platform.proxy

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.fifthlight.touchcontroller.platform.Platform
import top.fifthlight.touchcontroller.proxy.message.ProxyMessage
import top.fifthlight.touchcontroller.proxy.server.LauncherSocketProxyServer

class ProxyPlatform(scope: CoroutineScope, private val proxy: LauncherSocketProxyServer) : Platform {
    init {
        scope.launch {
            proxy.start()
        }
    }

    override fun pollEvent(): ProxyMessage? = proxy.receive()

    override fun sendEvent(message: ProxyMessage) {
        // UDP backend don't support sending message
    }
}
