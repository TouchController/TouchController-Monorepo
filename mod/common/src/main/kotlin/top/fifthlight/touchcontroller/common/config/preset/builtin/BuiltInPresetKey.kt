package top.fifthlight.touchcontroller.common.config.preset.builtin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.fifthlight.combine.data.Identifier
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.assets.TextureSet
import top.fifthlight.touchcontroller.common.config.controllerLayoutOf
import top.fifthlight.touchcontroller.common.config.preset.LayoutPreset
import top.fifthlight.touchcontroller.common.config.preset.PresetControlInfo
import top.fifthlight.touchcontroller.common.control.*

@Serializable
data class BuiltInPresetKey(
    @SerialName("texture_set")
    val textureSet: TextureSet.TextureSetKey = TextureSet.TextureSetKey.CLASSIC,
    @SerialName("control_style")
    val controlStyle: ControlStyle = ControlStyle.TouchGesture,
    @SerialName("move_method")
    val moveMethod: MoveMethod = MoveMethod.Dpad(),
    @SerialName("sprint_button_location")
    val sprintButtonLocation: SprintButtonLocation = SprintButtonLocation.NONE,
    @SerialName("opacity")
    val opacity: Float = .6f,
    @SerialName("scale")
    val scale: Float = 1f,
    @SerialName("useVanillaChat")
    val useVanillaChat: Boolean = false,
) {
    @Serializable
    sealed class ControlStyle {
        @Serializable
        @SerialName("touch_gesture")
        data object TouchGesture : ControlStyle()

        @Serializable
        @SerialName("split_controls")
        data class SplitControls(
            val buttonInteraction: Boolean = true,
        ) : ControlStyle()
    }

    @Serializable
    enum class SprintButtonLocation(
        val nameId: Identifier,
    ) {
        @SerialName("none")
        NONE(Texts.SCREEN_MANAGE_CONTROL_PRESET_SPRINT_NONE),

        @SerialName("right_top")
        RIGHT_TOP(Texts.SCREEN_MANAGE_CONTROL_PRESET_SPRINT_RIGHT_TOP),

        @SerialName("right")
        RIGHT(Texts.SCREEN_MANAGE_CONTROL_PRESET_SPRINT_RIGHT),
    }

    @Serializable
    sealed class MoveMethod {
        @Serializable
        @SerialName("dpad")
        data class Dpad(
            val swapJumpAndSneak: Boolean = false,
        ) : MoveMethod()

        @Serializable
        @SerialName("joystick")
        data class Joystick(
            val triggerSprint: Boolean = false,
        ) : MoveMethod()
    }

    val preset by lazy {
        val layers = BuiltinLayers[textureSet]
        val sprintButton = when (sprintButtonLocation) {
            SprintButtonLocation.NONE -> null
            SprintButtonLocation.RIGHT_TOP -> layers.sprintRightTopButton
            SprintButtonLocation.RIGHT -> layers.sprintRightButton
        }
        LayoutPreset(
            name = "Built-in preset",
            controlInfo = PresetControlInfo(
                splitControls = controlStyle is ControlStyle.SplitControls,
                disableTouchGesture = controlStyle is ControlStyle.SplitControls && controlStyle.buttonInteraction,
            ),
            layout = controllerLayoutOf(
                if (useVanillaChat) {
                    layers.vanillaChatControlLayer
                } else {
                    layers.controlLayer
                },
                layers.interactionLayer.takeIf { controlStyle is ControlStyle.SplitControls && controlStyle.buttonInteraction },
                layers.normalLayer.getByKey(this) + sprintButton,
                layers.swimmingLayer.getByKey(this),
                layers.flyingLayer.getByKey(this),
                layers.onBoatLayer.getByKey(this),
                layers.onMinecartLayer.getByKey(this),
                layers.ridingOnEntityLayer.getByKey(this),
            )
        ).mapWidgets { widget ->
            when (widget) {
                is CustomWidget -> {
                    if ((widget.normalTexture as? ButtonTexture.Fixed)?.texture?.textureItem == TextureSet.TextureKey.Inventory) {
                        return@mapWidgets widget
                    }

                    fun scaleTexture(scale: Float, texture: ButtonTexture) = when (texture) {
                        is ButtonTexture.Fixed -> texture.copy(scale = texture.scale * scale)
                        else -> texture
                    }

                    fun scaleActiveTexture(scale: Float, texture: ButtonActiveTexture) = when (texture) {
                        is ButtonActiveTexture.Texture -> ButtonActiveTexture.Texture(
                            scaleTexture(
                                scale,
                                texture.texture
                            )
                        )

                        else -> texture
                    }
                    widget.copy(
                        normalTexture = scaleTexture(scale, widget.normalTexture),
                        activeTexture = scaleActiveTexture(scale, widget.activeTexture),
                    )
                }

                is DPad -> widget.copy(
                    size = widget.size * scale,
                    textureSet = textureSet,
                    padding = if (textureSet == TextureSet.TextureSetKey.CLASSIC || textureSet == TextureSet.TextureSetKey.CLASSIC_EXTENSION) 4 else -1,
                )

                is Joystick -> widget.copy(
                    size = widget.size * scale,
                    stickSize = widget.stickSize * scale,
                    textureSet = textureSet,
                )

                is BoatButton -> widget.copy(
                    size = widget.size * scale,
                    textureSet = textureSet,
                )
            }.cloneBase(
                opacity = opacity,
                offset = (widget.offset.toOffset() * scale).toIntOffset(),
            )
        }
    }

    companion object {
        val DEFAULT = BuiltInPresetKey()
    }
}