package top.fifthlight.touchcontroller.common.ui.model

import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.fifthlight.touchcontroller.common.gal.GameDispatcher

abstract class TouchControllerScreenModel : ScreenModel, KoinComponent {
    private val gameDispatcher: GameDispatcher by inject()

    val coroutineScope = CoroutineScope(SupervisorJob() + gameDispatcher)

    override fun onDispose() {
        super.onDispose()
        coroutineScope.cancel()
    }
}