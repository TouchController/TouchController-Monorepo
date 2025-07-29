package top.fifthlight.touchcontroller.common.event

import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import top.fifthlight.combine.input.input.TextInputState
import top.fifthlight.combine.input.input.TextRange
import top.fifthlight.combine.paint.Canvas
import top.fifthlight.data.IntOffset
import top.fifthlight.data.Offset
import top.fifthlight.touchcontroller.common.config.GlobalConfigHolder
import top.fifthlight.touchcontroller.common.config.LayerConditionKey
import top.fifthlight.touchcontroller.common.gal.*
import top.fifthlight.touchcontroller.common.helper.fixAspectRadio
import top.fifthlight.touchcontroller.common.input.InputManager
import top.fifthlight.touchcontroller.common.layout.Context
import top.fifthlight.touchcontroller.common.layout.ContextInput
import top.fifthlight.touchcontroller.common.layout.DrawQueue
import top.fifthlight.touchcontroller.common.layout.Hud
import top.fifthlight.touchcontroller.common.model.ControllerHudModel
import top.fifthlight.touchcontroller.common.model.TouchStateModel
import top.fifthlight.touchcontroller.common.platform.PlatformProvider
import top.fifthlight.touchcontroller.proxy.client.PlatformCapability
import top.fifthlight.touchcontroller.proxy.message.*
import java.util.*

object RenderEvents : KoinComponent {
    private val logger = LoggerFactory.getLogger(RenderEvents::class.java)
    private val window: WindowHandle by inject()
    private val configHolder: GlobalConfigHolder by inject()
    private val controllerHudModel: ControllerHudModel by inject()
    private val touchStateModel: TouchStateModel by inject()
    private val playerHandleFactory: PlayerHandleFactory by inject()
    private val platformProvider: PlatformProvider by inject()
    private val gameStateProvider: GameStateProvider by inject()
    private val keyBindingHandler: KeyBindingHandler by inject()
    private val viewActionProvider: ViewActionProvider by inject()
    private var prevWidth = 0
    private var prevHeight = 0

    private val _platformCapabilities = mutableSetOf<PlatformCapability>()
    val platformCapabilities: Set<PlatformCapability> = Collections.unmodifiableSet(_platformCapabilities)

    @JvmStatic
    fun onRenderStart() {
        controllerHudModel.timer.renderTick()
        keyBindingHandler.renderTick(controllerHudModel.timer.renderTick)

        if (controllerHudModel.status.vibrate) {
            platformProvider.platform?.sendEvent(VibrateMessage(VibrateMessage.Kind.BLOCK_BROKEN))
            controllerHudModel.status.vibrate = false
        }

        val config = configHolder.config.value
        val playerHandle = playerHandleFactory.getPlayerHandle()
        val gameState = gameStateProvider.currentState()
        val platform = platformProvider.platform
        if (platform != null) {
            while (true) {
                val width = WindowEvents.windowWidth
                val height = WindowEvents.windowHeight
                if (width != prevWidth || height != prevHeight) {
                    prevWidth = width
                    prevHeight = height
                    platform.resize(width, height)
                }
                val message = platform.pollEvent() ?: break
                when (message) {
                    is AddPointerMessage -> {
                        touchStateModel.addPointer(
                            index = message.index,
                            position = Offset(
                                x = message.x,
                                y = message.y,
                            )
                        )
                    }

                    is RemovePointerMessage -> {
                        touchStateModel.removePointer(message.index)
                    }

                    ClearPointerMessage -> touchStateModel.clearPointer()

                    is CapabilityMessage -> {
                        PlatformCapability.entries.firstOrNull { it.id == message.capability }?.let {
                            logger.info("TouchController capability ${message.capability} set to ${message.enabled}")
                            if (message.enabled) {
                                _platformCapabilities += it
                            } else {
                                _platformCapabilities -= it
                            }
                        } ?: run {
                            logger.warn("Unknown capability: ${message.capability}, maybe you should update TouchController?")
                        }
                    }

                    is InputStatusMessage -> {
                        message.status?.let { status ->
                            InputManager.updateNativeState(
                                TextInputState(
                                    text = status.text,
                                    composition = TextRange(status.composition.start, status.composition.length),
                                    selection = TextRange(status.selection.start, status.selection.length),
                                    selectionLeft = status.selectionLeft,
                                )
                            )
                        }
                    }

                    is MoveViewMessage -> {
                        playerHandle?.let {
                            val rawDelta = Offset(
                                x = message.deltaYaw,
                                y = message.deltaPitch,
                            )
                            val offset = if (message.screenBased) {
                                rawDelta.fixAspectRadio(window.size) * config.control.viewMovementSensitivity
                            } else {
                                rawDelta
                            }
                            playerHandle.changeLookDirection(
                                deltaYaw = offset.x.toDouble(),
                                deltaPitch = offset.y.toDouble(),
                            )
                        }
                    }

                    else -> {}
                }
            }
        }

        if (config.debug.enableTouchEmulation) {
            val mousePosition = window.mousePosition
            if (window.mouseLeftPressed && mousePosition != null) {
                touchStateModel.addPointer(
                    index = 0,
                    position = mousePosition / window.size.toSize()
                )
            } else {
                touchStateModel.clearPointer()
            }
        }

        if (!gameState.inGame) {
            return
        }
        val player = playerHandle ?: return
        if (player.isFlying || player.isSubmergedInWater) {
            keyBindingHandler.getState(DefaultKeyBindingType.SNEAK).clearLock()
        }

        val preset = configHolder.currentPreset.value
        val currentPresetUuid = configHolder.currentPresetUuid.value
        if (controllerHudModel.status.previousPresetUuid != currentPresetUuid) {
            controllerHudModel.status.previousPresetUuid = currentPresetUuid
            controllerHudModel.status.enabledCustomConditions.clear()
        }
        val ridingType = player.ridingEntityType
        val condition = buildMap {
            put(LayerConditionKey.FLYING, player.isFlying)
            put(LayerConditionKey.CAN_FLY, player.canFly)
            put(LayerConditionKey.SWIMMING, player.isTouchingWater)
            put(LayerConditionKey.UNDERWATER, player.isSubmergedInWater)
            put(LayerConditionKey.SPRINTING, player.isSprinting)
            put(LayerConditionKey.SNEAKING, player.isSneaking)
            put(LayerConditionKey.ON_GROUND, player.onGround)
            put(LayerConditionKey.NOT_ON_GROUND, !player.onGround)
            put(LayerConditionKey.USING_ITEM, player.isUsingItem)
            put(LayerConditionKey.RIDING, ridingType != null)
            put(LayerConditionKey.BLOCK_SELECTED, viewActionProvider.getCrosshairTarget() == CrosshairTarget.BLOCK)
            put(LayerConditionKey.ON_MINECART, ridingType == RidingEntityType.MINECART)
            put(LayerConditionKey.ON_BOAT, ridingType == RidingEntityType.BOAT)
            put(LayerConditionKey.ON_PIG, ridingType == RidingEntityType.PIG)
            put(LayerConditionKey.ON_HORSE, ridingType == RidingEntityType.HORSE)
            put(LayerConditionKey.ON_CAMEL, ridingType == RidingEntityType.CAMEL)
            put(LayerConditionKey.ON_LLAMA, ridingType == RidingEntityType.LLAMA)
            put(LayerConditionKey.ON_STRIDER, ridingType == RidingEntityType.STRIDER)
        }.toPersistentMap()

        val drawQueue = DrawQueue()
        val result = Context(
            windowSize = window.size,
            windowScaledSize = window.scaledSize,
            drawQueue = drawQueue,
            size = window.scaledSize,
            screenOffset = IntOffset.ZERO,
            pointers = touchStateModel.pointers,
            input = ContextInput(
                inGui = gameState.inGui,
                condition = condition,
                customCondition = controllerHudModel.status.enabledCustomConditions.toPersistentSet(),
                perspective = gameState.perspective,
            ),
            status = controllerHudModel.status,
            timer = controllerHudModel.timer,
            keyBindingHandler = keyBindingHandler,
            config = configHolder.config.value,
            presetControlInfo = preset.controlInfo,
        ).run {
            Hud(layers = preset.layout)
            result
        }
        controllerHudModel.result = result
        controllerHudModel.pendingDrawQueue = drawQueue

        val status = controllerHudModel.status
        status.doubleClickCounter.clean(controllerHudModel.timer.clientTick)
        result.pendingAction.forEach { it.invoke(controllerHudModel.timer, player) }
        result.lookDirection?.let { (x, y) ->
            player.changeLookDirection(x.toDouble(), y.toDouble())
        }
        result.inventory.slots.forEachIndexed { index, slot ->
            if (slot.select) {
                player.currentSelectedSlot = index
            }
            if (slot.drop) {
                val stack = player.getInventorySlot(index)
                if (stack.isEmpty) {
                    player.currentSelectedSlot = index
                } else {
                    player.dropSlot(index)
                }
            }
        }
    }

    fun onHudRender(canvas: Canvas) {
        val queue = controllerHudModel.pendingDrawQueue
        queue?.let {
            queue.execute(canvas)
            controllerHudModel.pendingDrawQueue = null
        }
    }

    fun shouldRenderCrosshair(): Boolean {
        val config = configHolder.config.value
        val preset = configHolder.currentPreset.value
        if (!preset.controlInfo.disableCrosshair) {
            return true
        }
        val player = playerHandleFactory.getPlayerHandle() ?: return false
        return player.hasItemsOnHand(config.item.showCrosshairItems)
    }
}