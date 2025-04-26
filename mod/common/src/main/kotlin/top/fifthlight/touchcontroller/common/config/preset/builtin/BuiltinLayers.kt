package top.fifthlight.touchcontroller.common.config.preset.builtin

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.koin.core.component.KoinComponent
import top.fifthlight.data.IntOffset
import top.fifthlight.touchcontroller.assets.TextureSet
import top.fifthlight.touchcontroller.common.config.*
import top.fifthlight.touchcontroller.common.control.*
import top.fifthlight.touchcontroller.common.layout.Align
import java.util.concurrent.ConcurrentHashMap

@ConsistentCopyVisibility
data class BuiltinLayers private constructor(
    private val textureSet: TextureSet.TextureSetKey,
): KoinComponent {
    data class Layers(
        val name: String,
        val condition: LayoutLayerCondition,
        val dpadNormal: PersistentList<ControllerWidget>,
        val dpadSwap: PersistentList<ControllerWidget> = dpadNormal,
        val dpadNormalButtonInteract: PersistentList<ControllerWidget> = dpadNormal,
        val dpadSwapButtonInteract: PersistentList<ControllerWidget> = dpadSwap,
        val joystick: PersistentList<ControllerWidget> = dpadNormal,
    ) {
        fun getByKey(key: BuiltInPresetKey) = LayoutLayer(
            name = name,
            condition = condition,
            widgets = when (val moveMethod = key.moveMethod) {
                is BuiltInPresetKey.MoveMethod.Dpad -> if (key.controlStyle is BuiltInPresetKey.ControlStyle.SplitControls && key.controlStyle.buttonInteraction) {
                    if (moveMethod.swapJumpAndSneak) {
                        dpadSwapButtonInteract
                    } else {
                        dpadNormalButtonInteract
                    }
                } else {
                    if (moveMethod.swapJumpAndSneak) {
                        dpadSwap
                    } else {
                        dpadNormal
                    }
                }

                is BuiltInPresetKey.MoveMethod.Joystick -> joystick.map {
                    if (it is Joystick) {
                        it.copy(triggerSprint = moveMethod.triggerSprint)
                    } else {
                        it
                    }
                }.toPersistentList()
            }
        )
    }

    private val widgets = BuiltInWidgets[textureSet]

    val controlLayer = LayoutLayer(
        name = "Control",
        condition = layoutLayerConditionOf(),
        widgets = persistentListOf(
            widgets.pause.copy(
                align = Align.CENTER_TOP,
                offset = IntOffset(-9, 0),
            ),
            widgets.chat.copy(
                align = Align.CENTER_TOP,
                offset = IntOffset(9, 0),
            ),
            widgets.inventory,
        )
    )

    val vanillaChatControlLayer = LayoutLayer(
        name = "Control",
        condition = layoutLayerConditionOf(),
        widgets = persistentListOf(
            widgets.pause.copy(
                align = Align.CENTER_TOP,
                offset = IntOffset(-9, 0),
            ),
            widgets.vanillaChat.copy(
                align = Align.CENTER_TOP,
                offset = IntOffset(9, 0),
            ),
            widgets.inventory,
        )
    )

    val interactionLayer = LayoutLayer(
        name = "Interaction",
        condition = layoutLayerConditionOf(),
        widgets = persistentListOf(
            widgets.attack.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(86, 70),
            ),
            widgets.use.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(22, 37),
            ),
        )
    )

    val sprintRightButton = widgets.sprint.copy(
        align = Align.RIGHT_BOTTOM,
        offset = IntOffset(42, 131),
    )

    val sprintRightTopButton = widgets.sprint.copy(
        align = Align.RIGHT_TOP,
        offset = IntOffset(42, 44),
    )

    val normalLayer = Layers(
        name = "Normal",
        condition = layoutLayerConditionOf(
            LayerConditionKey.SWIMMING to LayerConditionValue.NEVER,
            LayerConditionKey.FLYING to LayerConditionValue.NEVER,
            LayerConditionKey.RIDING to LayerConditionValue.NEVER,
        ),
        dpadNormal = persistentListOf(
            DPad.create(
                align = Align.LEFT_BOTTOM,
                offset = IntOffset(12, 16),
                extraButton = widgets.dpadSneakButton,
            ),
            widgets.jump.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(42, 68),
            ),
        ),
        dpadSwap = persistentListOf(
            DPad.create(
                align = Align.LEFT_BOTTOM,
                offset = IntOffset(12, 16),
                extraButton = widgets.dpadJumpButton,
            ),
            widgets.sneak.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(42, 68),
            ),
        ),
        dpadNormalButtonInteract = persistentListOf(
            DPad.create(
                align = Align.LEFT_BOTTOM,
                offset = IntOffset(12, 16),
            ),
            widgets.jump.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(22, 165),
            ),
            widgets.sneak.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(22, 102),
            ),
        ),
        dpadSwapButtonInteract = persistentListOf(
            DPad.create(
                align = Align.LEFT_BOTTOM,
                offset = IntOffset(12, 16),
            ),
            widgets.jump.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(22, 165),
            ),
            widgets.sneak.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(22, 102),
            ),
        ),
        joystick = persistentListOf(
            Joystick(
                align = Align.LEFT_BOTTOM,
                offset = IntOffset(29, 32),
            ),
            widgets.jump.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(22, 165),
            ),
            widgets.sneak.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(22, 102),
            ),
        ),
    )

    val swimmingLayer = Layers(
        name = "Swimming",
        condition = layoutLayerConditionOf(
            LayerConditionKey.RIDING to LayerConditionValue.NEVER,
            LayerConditionKey.FLYING to LayerConditionValue.NEVER,
            LayerConditionKey.SWIMMING to LayerConditionValue.WANT,
            LayerConditionKey.UNDERWATER to LayerConditionValue.WANT,
        ),
        dpadNormal = persistentListOf(
            DPad.create(
                align = Align.LEFT_BOTTOM,
                offset = IntOffset(12, 16),
            ),
            widgets.ascendSwimming.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(42, 68),
            ),
            widgets.descendSwimming.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(42, 18),
            )
        ),
        dpadNormalButtonInteract = persistentListOf(
            DPad.create(
                align = Align.LEFT_BOTTOM,
                offset = IntOffset(12, 16),
            ),
            widgets.ascendSwimming.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(22, 165),
            ),
            widgets.descendSwimming.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(22, 102),
            )
        ),
        joystick = persistentListOf(
            Joystick(
                align = Align.LEFT_BOTTOM,
                offset = IntOffset(29, 32),
            ),
            widgets.ascendSwimming.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(22, 165),
            ),
            widgets.descendSwimming.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(22, 102),
            )
        ),
    )

    val flyingLayer = Layers(
        name = "Flying",
        condition = layoutLayerConditionOf(
            LayerConditionKey.FLYING to LayerConditionValue.REQUIRE,
            LayerConditionKey.RIDING to LayerConditionValue.NEVER,
        ),
        dpadNormal = persistentListOf(
            DPad.create(
                align = Align.LEFT_BOTTOM,
                offset = IntOffset(12, 16),
            ),
            widgets.ascendFlying.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(42, 68),
            ),
            widgets.descendFlying.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(42, 18),
            )
        ),
        dpadNormalButtonInteract = persistentListOf(
            DPad.create(
                align = Align.LEFT_BOTTOM,
                offset = IntOffset(12, 16),
            ),
            widgets.ascendFlying.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(22, 164),
            ),
            widgets.descendFlying.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(22, 102),
            )
        ),
        joystick = persistentListOf(
            Joystick(
                align = Align.LEFT_BOTTOM,
                offset = IntOffset(29, 32),
            ),
            widgets.ascendFlying.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(22, 165),
            ),
            widgets.descendFlying.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(22, 102),
            ),
        ),
    )

    val onMinecartLayer = Layers(
        name = "On minecart",
        condition = layoutLayerConditionOf(
            LayerConditionKey.ON_MINECART to LayerConditionValue.REQUIRE,
        ),
        dpadNormal = persistentListOf(
            widgets.forward.copy(
                align = Align.LEFT_BOTTOM,
                offset = IntOffset(59, 111),
            ),
            widgets.dismount.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(42, 68),
            ),
        ),
        dpadSwap = persistentListOf(
            widgets.forward.copy(
                align = Align.LEFT_BOTTOM,
                offset = IntOffset(59, 111)
            ),
            widgets.dismount.copy(
                align = Align.LEFT_BOTTOM,
                offset = IntOffset(59, 63),
            ),
        ),
        joystick = persistentListOf(
            Joystick(
                align = Align.LEFT_BOTTOM,
                offset = IntOffset(29, 32),
            ),
            widgets.dismount.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(22, 165),
            ),
        ),
    )

    val onBoatLayer = Layers(
        name = "On boat",
        condition = layoutLayerConditionOf(
            LayerConditionKey.ON_BOAT to LayerConditionValue.REQUIRE,
        ),
        dpadNormal = persistentListOf(
            BoatButton(
                align = Align.LEFT_BOTTOM,
                offset = IntOffset(16, 16),
                side = BoatButtonSide.LEFT,
            ),
            BoatButton(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(16, 16),
                side = BoatButtonSide.RIGHT,
            ),
            widgets.dismount.copy(
                align = Align.CENTER_BOTTOM,
                offset = IntOffset(0, 24),
            ),
        ),
        joystick = persistentListOf(
            Joystick(
                align = Align.LEFT_BOTTOM,
                offset = IntOffset(29, 32),
            ),
            widgets.dismount.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(22, 165),
            ),
        ),
    )

    val ridingOnEntityLayer = Layers(
        name = "Riding on entity",
        condition = layoutLayerConditionOf(
            LayerConditionKey.RIDING to LayerConditionValue.REQUIRE,
            LayerConditionKey.ON_BOAT to LayerConditionValue.NEVER,
            LayerConditionKey.ON_MINECART to LayerConditionValue.NEVER,
        ),
        dpadNormal = persistentListOf(
            DPad.create(
                align = Align.LEFT_BOTTOM,
                offset = IntOffset(12, 16),
                extraButton = widgets.dpadDismountButton,
            ),
            widgets.jumpHorse.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(42, 68),
            ),
        ),
        dpadSwap = persistentListOf(
            DPad.create(
                align = Align.LEFT_BOTTOM,
                offset = IntOffset(12, 16),
                extraButton = widgets.dpadJumpButtonWithoutLocking,
            ),
            widgets.dismount.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(42, 68),
            ),
        ),
        dpadNormalButtonInteract = persistentListOf(
            DPad.create(
                align = Align.LEFT_BOTTOM,
                offset = IntOffset(12, 16),
            ),
            widgets.jumpHorse.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(22, 165),
            ),
            widgets.dismount.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(22, 102),
            ),
        ),
        joystick = persistentListOf(
            Joystick(
                align = Align.LEFT_BOTTOM,
                offset = IntOffset(29, 32),
            ),
            widgets.jumpHorse.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(22, 165),
            ),
            widgets.dismount.copy(
                align = Align.RIGHT_BOTTOM,
                offset = IntOffset(22, 102),
            ),
        ),
    )

    companion object {
        private val cache = ConcurrentHashMap<TextureSet.TextureSetKey, BuiltinLayers>()
        operator fun get(textureSet: TextureSet.TextureSetKey): BuiltinLayers = cache.computeIfAbsent(textureSet, ::BuiltinLayers)
    }
}
