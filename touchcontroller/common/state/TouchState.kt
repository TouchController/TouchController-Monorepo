package top.fifthlight.touchcontroller.common.state

import top.fifthlight.data.Offset
import kotlin.uuid.Uuid

sealed class PointerState {
    data object New : PointerState()

    data class View(
        val initialPosition: Offset,
        val lastPosition: Offset,
        val moving: Boolean = false,
        val viewState: ViewPointerState,
        val pressTime: Int,
    ) : PointerState() {
        enum class ViewPointerState {
            INITIAL,
            CONSUMED,
            BREAKING,
            USING,
        }
    }

    data object Joystick : PointerState()

    data object Invalid : PointerState()

    data class Released(
        val previousPosition: Offset,
        val previousState: PointerState
    ) : PointerState() {
        init {
            require(previousState !is Released)
        }
    }

    data class Button(
        val id: Uuid,
        val initialPosition: Offset,
        val lastPosition: Offset,
        val moving: Boolean,
        val swipe: Boolean,
    ) : PointerState()

    data class InventorySlot(val index: Int, val startTick: Int) : PointerState()
}

data class Pointer(
    var position: Offset,
    var state: PointerState = PointerState.New
)
