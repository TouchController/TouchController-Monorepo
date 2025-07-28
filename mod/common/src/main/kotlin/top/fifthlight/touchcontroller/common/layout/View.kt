package top.fifthlight.touchcontroller.common.layout

import org.koin.core.component.get
import top.fifthlight.touchcontroller.common.ext.fastRandomUuid
import top.fifthlight.touchcontroller.common.gal.CrosshairTarget
import top.fifthlight.touchcontroller.common.gal.DefaultKeyBindingType
import top.fifthlight.touchcontroller.common.gal.PlayerHandleFactory
import top.fifthlight.touchcontroller.common.gal.ViewActionProvider
import top.fifthlight.touchcontroller.common.helper.fixAspectRadio
import top.fifthlight.touchcontroller.common.state.PointerState

private val viewUuid = fastRandomUuid()

fun Context.View() {
    val viewActionProvider: ViewActionProvider = get()

    val attackKeyState = keyBindingHandler.getState(DefaultKeyBindingType.ATTACK)
    val useKeyState = keyBindingHandler.getState(DefaultKeyBindingType.USE)

    attackKeyState.refreshLock(viewUuid, timer.renderTick)
    useKeyState.refreshLock(viewUuid, timer.renderTick)

    var releasedView = false
    for (key in pointers.keys.toList()) {
        val state = pointers[key]!!.state
        if (state is PointerState.Released) {
            if (state.previousState is PointerState.View) {
                val previousState = state.previousState
                when (previousState.viewState) {
                    PointerState.View.ViewPointerState.CONSUMED -> {}
                    PointerState.View.ViewPointerState.BREAKING -> {
                        attackKeyState.clearLock(viewUuid)
                    }

                    PointerState.View.ViewPointerState.USING -> {
                        useKeyState.clearLock(viewUuid)
                    }

                    PointerState.View.ViewPointerState.INITIAL -> {
                        if (!releasedView) {
                            val pressTime = timer.clientTick - previousState.pressTime
                            // Pressed less than time threshold and not moving, recognized as short click
                            if (pressTime < config.control.viewHoldDetectTicks && !previousState.moving) {
                                val crosshairTarget = viewActionProvider.getCrosshairTarget() ?: break
                                when (crosshairTarget) {
                                    CrosshairTarget.BLOCK, CrosshairTarget.MISS -> {
                                        // Short click on block or air: use item
                                        useKeyState.click()
                                    }

                                    CrosshairTarget.ENTITY -> {
                                        // Short click on entity: attack the entity
                                        attackKeyState.click()
                                    }
                                }
                            }
                        }
                    }
                }
                releasedView = true
            }
            // Remove all released pointers, because View is the last layout
            pointers.remove(key)
        }
    }

    var currentViewPointer = pointers.values.firstOrNull {
        it.state is PointerState.View
    }

    currentViewPointer?.let { pointer ->
        val state = pointer.state as PointerState.View

        // Drop all unhandled pointers
        pointers.values.forEach {
            when (it.state) {
                PointerState.New -> it.state = PointerState.Invalid
                else -> {}
            }
        }

        var moving = state.moving
        if (!state.moving) {
            // Move detect
            val delta = (pointer.position - state.initialPosition).fixAspectRadio(windowSize).squaredLength
            val threshold = config.control.viewHoldDetectThreshold * 0.01f
            if (delta > threshold * threshold) {
                moving = true
            }
        }

        val movement = (pointer.position - state.lastPosition).fixAspectRadio(windowSize)
        result.lookDirection = movement * config.control.viewMovementSensitivity

        val playerHandleFactory: PlayerHandleFactory = get()
        val player = playerHandleFactory.getPlayerHandle()
        // Consume the pointer if player is null or touch gesture is disabled
        if (player == null || presetControlInfo.disableTouchGesture) {
            pointer.state = state.copy(
                lastPosition = pointer.position,
                moving = moving,
                viewState = PointerState.View.ViewPointerState.CONSUMED
            )
            return@let
        }

        // Early exit for consumed pointer
        if (state.viewState == PointerState.View.ViewPointerState.CONSUMED) {
            pointer.state = state.copy(lastPosition = pointer.position, moving = moving)
            return@let
        }

        val pressTime = timer.clientTick - state.pressTime
        var viewState = state.viewState
        val crosshairTarget = viewActionProvider.getCrosshairTarget()
        val itemUsable = player.hasItemsOnHand(config.item.usableItems)

        // If pointer kept still and held for hold-detecting ticks in config
        if (viewState == PointerState.View.ViewPointerState.INITIAL && pressTime >= config.control.viewHoldDetectTicks && !moving) {
            viewState = if (itemUsable) {
                // Trigger item long click
                useKeyState.addLock(viewUuid, timer.renderTick)
                PointerState.View.ViewPointerState.USING
            } else {
                when (crosshairTarget) {
                    CrosshairTarget.BLOCK -> {
                        // Trigger block breaking
                        attackKeyState.addLock(viewUuid, timer.renderTick)
                        PointerState.View.ViewPointerState.BREAKING
                    }

                    CrosshairTarget.ENTITY -> {
                        // Trigger item use once and consume
                        useKeyState.click()
                        PointerState.View.ViewPointerState.CONSUMED
                    }

                    CrosshairTarget.MISS, null -> PointerState.View.ViewPointerState.CONSUMED
                }
            }
        }

        pointer.state = state.copy(
            lastPosition = pointer.position,
            moving = moving,
            viewState = viewState
        )
    } ?: run {
        pointers.values.forEach {
            when (it.state) {
                PointerState.New -> {
                    if (currentViewPointer != null) {
                        it.state = PointerState.Invalid
                    } else {
                        it.state = PointerState.View(
                            initialPosition = it.position,
                            lastPosition = it.position,
                            pressTime = timer.clientTick,
                            viewState = PointerState.View.ViewPointerState.INITIAL
                        )
                        currentViewPointer = it
                    }
                }

                else -> {}
            }
        }
    }

    currentViewPointer?.let { pointer ->
        result.showBlockOutline = true
        // Update current view pointer
        if (!presetControlInfo.splitControls) {
            result.crosshairStatus = CrosshairStatus(
                position = pointer.position,
                breakPercent = viewActionProvider.getCurrentBreakingProgress(),
            )
        }
    } ?: run {
        if (attackKeyState.haveClickCount() || useKeyState.haveClickCount()) {
            // Keep last crosshair status for key handling
            result.crosshairStatus = status.lastCrosshairStatus
        }
    }

    if (presetControlInfo.splitControls) {
        result.showBlockOutline = true
    }

    status.lastCrosshairStatus = result.crosshairStatus
}