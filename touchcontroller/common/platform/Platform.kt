package top.fifthlight.touchcontroller.common.platform

import top.fifthlight.touchcontroller.proxy.message.ProxyMessage

interface Platform {
    fun resize(width: Int, height: Int) {}
    fun pollEvent(): ProxyMessage?
    fun sendEvent(message: ProxyMessage)
}
