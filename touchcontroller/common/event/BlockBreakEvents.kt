package top.fifthlight.touchcontroller.common.event

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.fifthlight.touchcontroller.common.config.GlobalConfigHolder
import top.fifthlight.touchcontroller.common.model.ControllerHudModel

object BlockBreakEvents : KoinComponent {
    private val controllerHudModel: ControllerHudModel by inject()
    private val configHolder: GlobalConfigHolder by inject()

    fun afterBlockBreak() {
        if (configHolder.config.value.regular.vibration) {
            controllerHudModel.status.vibrate = true
        }
    }
}