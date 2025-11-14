package top.fifthlight.touchcontroller.common.model

import org.koin.core.component.KoinComponent
import top.fifthlight.touchcontroller.common.layout.ContextResult
import top.fifthlight.touchcontroller.common.layout.ContextStatus
import top.fifthlight.touchcontroller.common.layout.ContextTimer
import top.fifthlight.touchcontroller.common.layout.DrawQueue

class ControllerHudModel : KoinComponent {
    var result = ContextResult()
    val status = ContextStatus()
    val timer = ContextTimer()
    var pendingDrawQueue: DrawQueue? = null
}
