package top.fifthlight.touchcontroller.common.event

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.fifthlight.touchcontroller.common.gal.KeyBindingHandler
import top.fifthlight.touchcontroller.common.model.ControllerHudModel

object TickEvents : KoinComponent {
    private val controllerHudModel: ControllerHudModel by inject()
    private val keyBindingHandler: KeyBindingHandler by inject()

    // Client side tick, neither server tick nor client render tick
    fun clientTick() {
        controllerHudModel.timer.clientTick()
        keyBindingHandler.clientTick(controllerHudModel.timer.clientTick)
    }
}
