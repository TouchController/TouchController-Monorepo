package top.fifthlight.touchcontroller.common.layout

import top.fifthlight.data.Offset
import top.fifthlight.touchcontroller.common.helper.fixAspectRadio
import top.fifthlight.touchcontroller.common.state.PointerState
import kotlin.uuid.Uuid

data class ButtonResult(
    val newPointer: Boolean = false,
    val clicked: Boolean = false,
    val release: Boolean = false,
)

fun Context.SwipeButton(
    id: Uuid,
    content: Context.(clicked: Boolean) -> Unit,
) = Button(
    id = id,
    swipe = true,
    content = content,
)

fun Context.Button(
    id: Uuid,
    grabTrigger: Boolean = false,
    swipe: Boolean = false,
    moveView: Boolean = false,
    content: Context.(clicked: Boolean) -> Unit,
): ButtonResult {
    var newPointer = false
    var clicked = false
    var release = false
    for (pointer in pointers.values) {
        when (val state = pointer.state) {
            PointerState.New -> {
                if (pointer.inRect(size)) {
                    pointer.state = PointerState.Button(
                        id = id,
                        swipe = swipe,
                        initialPosition = pointer.position,
                        lastPosition = pointer.position,
                        moving = false,
                    )
                    newPointer = true
                    clicked = true
                }
            }

            is PointerState.Button -> {
                if (moveView && state.id == id) {
                    var moving = state.moving
                    if (!moving) {
                        // Move detect
                        val delta = (pointer.position - state.initialPosition).fixAspectRadio(windowSize).squaredLength
                        val threshold = config.control.viewHoldDetectThreshold * 0.01f
                        if (delta > threshold * threshold) {
                            moving = true
                        }
                    } else {
                        val movement = (pointer.position - state.lastPosition).fixAspectRadio(windowSize)
                        result.lookDirection =
                            (result.lookDirection ?: Offset.ZERO) + movement * config.control.viewMovementSensitivity
                    }
                    pointer.state = state.copy(
                        moving = moving,
                        lastPosition = pointer.position,
                    )
                }
                if (swipe) {
                    if (pointer.inRect(size)) {
                        clicked = true
                    }
                } else if (state.id == id) {
                    if (grabTrigger || pointer.inRect(size)) {
                        clicked = true
                    }
                }
            }

            is PointerState.Released -> {
                val previousState = state.previousState
                if (previousState is PointerState.Button && previousState.id == id) {
                    release = true
                }
            }

            else -> {}
        }
    }

    content(clicked)
    return ButtonResult(
        newPointer = newPointer,
        clicked = clicked,
        release = release
    )
}
