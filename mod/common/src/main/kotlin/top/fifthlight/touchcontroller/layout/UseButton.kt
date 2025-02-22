package top.fifthlight.touchcontroller.layout

import top.fifthlight.combine.paint.Color
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.control.UseButton
import top.fifthlight.touchcontroller.control.UseButtonTexture
import top.fifthlight.touchcontroller.control.UseButtonTrigger
import top.fifthlight.touchcontroller.gal.KeyBindingType
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun Context.UseButton(config: UseButton) {
    val useButtonState = keyBindingHandler.getState(KeyBindingType.USE)
    val (newPointer, clicked) = Button(id = config.id) { clicked ->
        val isLockTrigger = config.trigger == UseButtonTrigger.SINGLE_CLICK_LOCK
        val locked = useButtonState.locked
        withAlign(align = Align.CENTER_CENTER, size = size) {
            when (config.texture) {
                UseButtonTexture.CLASSIC -> {
                    if (isLockTrigger && locked) {
                        if (clicked) {
                            Texture(texture = Textures.CONTROL_CLASSIC_USE_USE_ACTIVE, tint = Color(0xFFAAAAAAu))
                        } else {
                            Texture(texture = Textures.CONTROL_CLASSIC_USE_USE_ACTIVE)
                        }
                    } else {
                        if (clicked) {
                            Texture(texture = Textures.CONTROL_CLASSIC_USE_USE, tint = Color(0xFFAAAAAAu))
                        } else {
                            Texture(texture = Textures.CONTROL_CLASSIC_USE_USE)
                        }
                    }
                }

                UseButtonTexture.NEW -> {
                    if (clicked || locked) {
                        Texture(texture = Textures.CONTROL_NEW_USE_USE_ACTIVE)
                    } else {
                        Texture(texture = Textures.CONTROL_NEW_USE_USE)
                    }
                }
            }
        }
    }

    when (config.trigger) {
        UseButtonTrigger.SINGLE_CLICK_LOCK -> if (newPointer) {
            useButtonState.locked = !useButtonState.locked
        }

        UseButtonTrigger.HOLD -> {
            if (clicked) {
                useButtonState.clicked = true
            }
        }
    }
}